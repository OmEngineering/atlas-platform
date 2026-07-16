package com.atlas.modules.comments.controller;

import com.atlas.modules.comments.dto.CommentResponse;
import com.atlas.modules.comments.dto.CreateCommentRequest;
import com.atlas.modules.comments.service.CommentService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@Tag(name = "Comments")
public class TaskCommentController {

    private final CommentService commentService;
    private final SecurityContextAccessor securityContextAccessor;

    public TaskCommentController(CommentService commentService, SecurityContextAccessor securityContextAccessor) {
        this.commentService = commentService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> create(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.of(commentService.create(
                taskId, securityContextAccessor.currentUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<CommentResponse>> list(@PathVariable UUID taskId) {
        return ApiResponse.of(commentService.listForTask(
                taskId, securityContextAccessor.currentUserId()));
    }
}
