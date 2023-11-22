package ali.task.tracker.store.service;

import ali.task.tracker.api.dto.AckDto;
import ali.task.tracker.api.dto.ProjectDto;

import java.util.List;
import java.util.Optional;
//
public interface ProjectService {
    AckDto deleteProject(Long projectId);

    ProjectDto createOrUpdateProject(Optional<Long> optionalProjectId, Optional<String> optionalProjectName);

    List<ProjectDto> fetchProjects(Optional<String> optionalPrefixName);
}
