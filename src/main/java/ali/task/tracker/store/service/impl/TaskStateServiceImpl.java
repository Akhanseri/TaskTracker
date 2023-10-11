package ali.task.tracker.store.service.impl;

import ali.task.tracker.api.controllers.helpers.ControllerHelper;
import ali.task.tracker.api.dto.AckDto;
import ali.task.tracker.api.dto.TaskStateDto;
import ali.task.tracker.api.exceptions.BadRequestException;
import ali.task.tracker.api.exceptions.NotFoundException;
import ali.task.tracker.api.factories.TaskStateDtoFactory;
import ali.task.tracker.store.entities.ProjectEntity;
import ali.task.tracker.store.entities.TaskStateEntity;
import ali.task.tracker.store.repositories.TaskStateRepository;
import ali.task.tracker.store.service.TaskStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskStateServiceImpl implements TaskStateService {

    private final TaskStateRepository taskStateRepository;
    private final TaskStateDtoFactory taskStateDtoFactory;
    private final ControllerHelper controllerHelper;

    @Autowired
    public TaskStateServiceImpl(TaskStateRepository taskStateRepository, TaskStateDtoFactory taskStateDtoFactory, ControllerHelper controllerHelper) {
        this.taskStateRepository = taskStateRepository;
        this.taskStateDtoFactory = taskStateDtoFactory;
        this.controllerHelper = controllerHelper;
    }

    @Override
    public List<TaskStateDto> getTaskStates(Long projectId) {
        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        return project.getTaskStates()
                .stream()
                .map(taskStateDtoFactory::makeTaskStateDto)
                .collect(Collectors.toList());
    }

    @Override
    public TaskStateDto createTaskState(Long projectId, String taskStateName) {
        if (taskStateName.trim().isEmpty()) {
            throw new BadRequestException("Task state name can't be empty.");
        }

        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        Optional<TaskStateEntity> optionalAnotherTaskState = Optional.empty();

        for (TaskStateEntity taskState : project.getTaskStates()) {
            if (taskState.getName().equalsIgnoreCase(taskStateName)) {
                throw new BadRequestException(String.format("Task state \"%s\" already exists.", taskStateName));
            }
            if (taskState.getRightTaskState().isEmpty()) {
                optionalAnotherTaskState = Optional.of(taskState);
                break;
            }
        }

        TaskStateEntity taskState = taskStateRepository.saveAndFlush(
                TaskStateEntity.builder()
                        .name(taskStateName)
                        .project(project)
                        .build()
        );

        optionalAnotherTaskState.ifPresent(anotherTaskState -> {
            taskState.setLeftTaskState(anotherTaskState);
            anotherTaskState.setRightTaskState(taskState);
            taskStateRepository.saveAndFlush(anotherTaskState);
        });

        final TaskStateEntity savedTaskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeTaskStateDto(savedTaskState);
    }

    @Override
    public TaskStateDto updateTaskState(Long taskStateId, String taskStateName) {
        if (taskStateName.trim().isEmpty()) {
            throw new BadRequestException("Task state name can't be empty.");
        }

        TaskStateEntity taskState = getTaskStateOrThrowException(taskStateId);

        taskStateRepository.findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(
                taskState.getProject().getId(),
                taskStateName
        ).filter(anotherTaskState -> !anotherTaskState.getId().equals(taskStateId)).ifPresent(anotherTaskState -> {
            throw new BadRequestException(String.format("Task state \"%s\" already exists.", taskStateName));
        });

        taskState.setName(taskStateName);
        taskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeTaskStateDto(taskState);
    }

    @Override
    public TaskStateDto changeTaskStatePosition(Long taskStateId, Optional<Long> optionalLeftTaskStateId) {
        TaskStateEntity changeTaskState = getTaskStateOrThrowException(taskStateId);

        ProjectEntity project = changeTaskState.getProject();

        Optional<Long> optionalOldLeftTaskStateId = changeTaskState
                .getLeftTaskState()
                .map(TaskStateEntity::getId);

        if (optionalOldLeftTaskStateId.equals(optionalLeftTaskStateId)) {
            return taskStateDtoFactory.makeTaskStateDto(changeTaskState);
        }

        Optional<TaskStateEntity> optionalNewLeftTaskState = optionalLeftTaskStateId
                .map(leftTaskStateId -> {

                    if (taskStateId.equals(leftTaskStateId)) {
                        throw new BadRequestException("Left task state id equals changed task state.");
                    }

                    TaskStateEntity leftTaskStateEntity = getTaskStateOrThrowException(leftTaskStateId);

                    if (!project.getId().equals(leftTaskStateEntity.getProject().getId())) {
                        throw new BadRequestException("Task state position can be changed within the same project.");
                    }

                    return leftTaskStateEntity;
                });

        Optional<TaskStateEntity> optionalNewRightTaskState;
        if (optionalNewLeftTaskState.isEmpty()) {

            optionalNewRightTaskState = project
                    .getTaskStates()
                    .stream()
                    .filter(anotherTaskState -> anotherTaskState.getLeftTaskState().isEmpty())
                    .findAny();
        } else {

            optionalNewRightTaskState = optionalNewLeftTaskState
                    .get()
                    .getRightTaskState();
        }

        replaceOldTaskStatePosition(changeTaskState);

        if (optionalNewLeftTaskState.isPresent()) {

            TaskStateEntity newLeftTaskState = optionalNewLeftTaskState.get();

            newLeftTaskState.setRightTaskState(changeTaskState);

            changeTaskState.setLeftTaskState(newLeftTaskState);
        } else {
            changeTaskState.setLeftTaskState(null);
        }

        if (optionalNewRightTaskState.isPresent()) {

            TaskStateEntity newRightTaskState = optionalNewRightTaskState.get();

            newRightTaskState.setLeftTaskState(changeTaskState);

            changeTaskState.setRightTaskState(newRightTaskState);
        } else {
            changeTaskState.setRightTaskState(null);
        }

        changeTaskState = taskStateRepository.saveAndFlush(changeTaskState);

        optionalNewLeftTaskState.ifPresent(taskStateRepository::saveAndFlush);
        optionalNewRightTaskState.ifPresent(taskStateRepository::saveAndFlush);

        return taskStateDtoFactory.makeTaskStateDto(changeTaskState);
    }

    @Override
    public AckDto deleteTaskState(Long taskStateId) {
        TaskStateEntity changeTaskState = getTaskStateOrThrowException(taskStateId);

        replaceOldTaskStatePosition(changeTaskState);
        taskStateRepository.delete(changeTaskState);

        return AckDto.builder().answer(true).build();
    }

    private void replaceOldTaskStatePosition(TaskStateEntity changeTaskState) {
        Optional<TaskStateEntity> optionalOldLeftTaskState = changeTaskState.getLeftTaskState();
        Optional<TaskStateEntity> optionalOldRightTaskState = changeTaskState.getRightTaskState();

        optionalOldLeftTaskState.ifPresent(it -> {
            it.setRightTaskState(optionalOldRightTaskState.orElse(null));
            taskStateRepository.saveAndFlush(it);
        });

        optionalOldRightTaskState.ifPresent(it -> {
            it.setLeftTaskState(optionalOldLeftTaskState.orElse(null));
            taskStateRepository.saveAndFlush(it);
        });
    }

    private TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format(
                                        "Task state with \"%s\" id doesn't exist.",
                                        taskStateId
                                )
                        )
                );
    }
}
