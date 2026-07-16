package com.atlas.modules.tasks.controller;

import com.atlas.modules.tasks.dto.CreateTaskRequest;
import com.atlas.modules.tasks.dto.TaskResponse;
import com.atlas.modules.tasks.service.TaskService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
@Tag(name = "Tasks")
public class ProjectTaskController {

    private final TaskService taskService;
    private final SecurityContextAccessor securityContextAccessor;

    public ProjectTaskController(TaskService taskService, SecurityContextAccessor securityContextAccessor) {
        this.taskService = taskService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TaskResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.of(taskService.create(
                projectId, securityContextAccessor.currentUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<TaskResponse>> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID assignee,
            @RequestParam(required = false) UUID label,
            @RequestParam(required = false) UUID milestone) {
        return ApiResponse.of(taskService.listForProject(
                projectId,
                securityContextAccessor.currentUserId(),
                status,
                assignee,
                label,
                milestone));
    }
}
