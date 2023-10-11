package ali.task.tracker.api.controllers;

import ali.task.tracker.api.dto.AckDto;
import ali.task.tracker.api.dto.TaskStateDto;
import ali.task.tracker.store.service.TaskStateService;
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
public class TaskStateController {

    private final TaskStateService taskStateService;


    @GetMapping("GET_TASK_STATES")
    public List<TaskStateDto> getTaskStates(@PathVariable(name = "project_id") Long projectId) {
        return taskStateService.getTaskStates(projectId);
    }

    @PostMapping("CREATE_TASK_STATE")
    public TaskStateDto createTaskSate(
            @PathVariable(name = "project_id") Long projectId,
            @RequestParam(name = "task_state_name") String taskStateName) {
        return taskStateService.createTaskState(projectId, taskStateName);
    }

    @PatchMapping("UPDATE_TASK_STATE")
    public TaskStateDto updateTaskState(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_state_name") String taskStateName) {
        return taskStateService.updateTaskState(taskStateId, taskStateName);
    }

    @PatchMapping("CHANGE_TASK_STATE_POSITION")
    public TaskStateDto changeTaskStatePosition(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "left_task_state_id", required = false) Optional<Long> optionalLeftTaskStateId) {
        return taskStateService.changeTaskStatePosition(taskStateId, optionalLeftTaskStateId);
    }

    @DeleteMapping("DELETE_TASK_STATE")
    public AckDto deleteTaskState(@PathVariable(name = "task_state_id") Long taskStateId) {
        return taskStateService.deleteTaskState(taskStateId);
    }
}
