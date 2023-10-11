package ali.task.tracker.store.service.impl;

import ali.task.tracker.api.controllers.helpers.ControllerHelper;
import ali.task.tracker.api.dto.AckDto;
import ali.task.tracker.api.dto.ProjectDto;
import ali.task.tracker.api.exceptions.BadRequestException;
import ali.task.tracker.api.factories.ProjectDtoFactory;
import ali.task.tracker.store.entities.ProjectEntity;
import ali.task.tracker.store.repositories.ProjectRepository;
import ali.task.tracker.store.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ControllerHelper controllerHelper;
    private final ProjectDtoFactory projectDtoFactory;

    @Override
    public ProjectDto createOrUpdateProject(Optional<Long> optionalProjectId, Optional<String> optionalProjectName) {
        optionalProjectName = optionalProjectName.filter(projectName -> !projectName.trim().isEmpty());
        boolean isCreate = optionalProjectId.isEmpty();

        if (isCreate && optionalProjectName.isEmpty()) {
            throw new BadRequestException("Project name can't be empty.");
        }

        final ProjectEntity project = optionalProjectId
                .map(controllerHelper::getProjectOrThrowException)
                .orElseGet(() -> ProjectEntity.builder().build());

        optionalProjectName.ifPresent(projectName -> {
            projectRepository
                    .findByName(projectName)
                    .filter(anotherProject -> !Objects.equals(anotherProject.getId(), project.getId()))
                    .ifPresent(anotherProject -> {
                        throw new BadRequestException(
                                String.format("Project \"%s\" already exists.", projectName)
                        );
                    });

            project.setName(projectName);
        });

        final ProjectEntity savedProject = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(savedProject);
    }

    @Override
    public AckDto deleteProject(Long projectId) {
        controllerHelper.getProjectOrThrowException(projectId);
        projectRepository.deleteById(projectId);
        return AckDto.makeDefault(true);
    }

    @Override
    public List<ProjectDto> fetchProjects(Optional<String> optionalPrefixName) {
        optionalPrefixName = optionalPrefixName.filter(prefixName -> !prefixName.trim().isEmpty());

        Stream<ProjectEntity> projectStream = optionalPrefixName
                .map(projectRepository::streamAllByNameStartsWithIgnoreCase)
                .orElseGet(projectRepository::streamAllBy);

        return projectStream
                .map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }
}
