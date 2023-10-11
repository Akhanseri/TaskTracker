package ali.task.tracker.api.controllers;

import ali.task.tracker.api.dto.AckDto;
import ali.task.tracker.api.dto.ProjectDto;
import ali.task.tracker.store.service.ProjectService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    @PutMapping("CREATE_OR_UPDATE_PROJECT")
    public ProjectDto createOrUpdateProject(
            @RequestParam(value = "project_id", required = false) Optional<Long> optionalProjectId,
            @RequestParam(value = "project_name", required = false) Optional<String> optionalProjectName) {
        return projectService.createOrUpdateProject(optionalProjectId, optionalProjectName);
    }

    @DeleteMapping("DELETE_PROJECT")
    public AckDto deleteProject(@PathVariable("project_id") Long projectId) {
        return projectService.deleteProject(projectId);
    }

    @GetMapping("FETCH_PROJECTS")
    public List<ProjectDto> fetchProjects(
            @RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName) {
        return projectService.fetchProjects(optionalPrefixName);
    }
}
