package ali.task.tracker.store.service;

import ali.task.tracker.api.dto.AckDto;
import ali.task.tracker.api.dto.TaskStateDto;

import java.util.List;
import java.util.Optional;

public interface TaskStateService {

    List<TaskStateDto> getTaskStates(Long projectId);

    TaskStateDto createTaskState(Long projectId, String taskStateName);

    TaskStateDto updateTaskState(Long taskStateId, String taskStateName);

    TaskStateDto changeTaskStatePosition(Long taskStateId, Optional<Long> optionalLeftTaskStateId);

    AckDto deleteTaskState(Long taskStateId);
}

