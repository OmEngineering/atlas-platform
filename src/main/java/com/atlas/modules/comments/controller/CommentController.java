package com.atlas.modules.comments.controller;

import com.atlas.modules.comments.dto.CommentResponse;
import com.atlas.modules.comments.dto.UpdateCommentRequest;
import com.atlas.modules.comments.service.CommentService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
@Tag(name = "Comments")
public class CommentController {

    private final CommentService commentService;
    private final SecurityContextAccessor securityContextAccessor;

    public CommentController(CommentService commentService, SecurityContextAccessor securityContextAccessor) {
        this.commentService = commentService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PatchMapping("/{id}")
    public ApiResponse<CommentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommentRequest request) {
        return ApiResponse.of(commentService.update(id, securityContextAccessor.currentUserId(), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        commentService.delete(id, securityContextAccessor.currentUserId());
    }
}
