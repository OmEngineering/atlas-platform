package com.atlas.modules.tasks.controller;

import com.atlas.modules.tasks.dto.AddAssigneeRequest;
import com.atlas.modules.tasks.dto.AddLabelRequest;
import com.atlas.modules.tasks.dto.TaskResponse;
import com.atlas.modules.tasks.dto.UpdateTaskRequest;
import com.atlas.modules.tasks.service.TaskService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService taskService;
    private final SecurityContextAccessor securityContextAccessor;

    public TaskController(TaskService taskService, SecurityContextAccessor securityContextAccessor) {
        this.taskService = taskService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> get(@PathVariable UUID id) {
        return ApiResponse.of(taskService.get(id, securityContextAccessor.currentUserId()));
    }

    @PatchMapping("/{id}")
    public ApiResponse<TaskResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.of(taskService.update(id, securityContextAccessor.currentUserId(), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        taskService.delete(id, securityContextAccessor.currentUserId());
    }

    @PostMapping("/{id}/assignees")
    public ApiResponse<TaskResponse> addAssignee(
            @PathVariable UUID id,
            @Valid @RequestBody AddAssigneeRequest request) {
        return ApiResponse.of(taskService.addAssignee(id, securityContextAccessor.currentUserId(), request));
    }

    @DeleteMapping("/{id}/assignees/{userId}")
    public ApiResponse<TaskResponse> removeAssignee(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        return ApiResponse.of(taskService.removeAssignee(id, userId, securityContextAccessor.currentUserId()));
    }

    @PostMapping("/{id}/labels")
    public ApiResponse<TaskResponse> addLabel(
            @PathVariable UUID id,
            @Valid @RequestBody AddLabelRequest request) {
        return ApiResponse.of(taskService.addLabel(id, securityContextAccessor.currentUserId(), request));
    }

    @DeleteMapping("/{id}/labels/{labelId}")
    public ApiResponse<TaskResponse> removeLabel(
            @PathVariable UUID id,
            @PathVariable UUID labelId) {
        return ApiResponse.of(taskService.removeLabel(id, labelId, securityContextAccessor.currentUserId()));
    }
}
