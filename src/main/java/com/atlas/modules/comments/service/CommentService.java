package com.atlas.modules.comments.service;

import com.atlas.exception.ForbiddenException;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.comments.dto.CommentResponse;
import com.atlas.modules.comments.dto.CreateCommentRequest;
import com.atlas.modules.comments.dto.UpdateCommentRequest;
import com.atlas.modules.comments.entity.Comment;
import com.atlas.modules.comments.mapper.CommentMapper;
import com.atlas.modules.comments.repository.CommentRepository;
import com.atlas.modules.projects.service.ProjectAuthorizationService;
import com.atlas.modules.tasks.entity.Task;
import com.atlas.modules.tasks.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CommentService {

    private static final Duration EDIT_WINDOW = Duration.ofMinutes(15);
    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\w+");

    private final CommentRepository commentRepository;
    private final TaskService taskService;
    private final ProjectAuthorizationService projectAuthorizationService;

    public CommentService(
            CommentRepository commentRepository,
            TaskService taskService,
            ProjectAuthorizationService projectAuthorizationService) {
        this.commentRepository = commentRepository;
        this.taskService = taskService;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    @Transactional
    public CommentResponse create(UUID taskId, UUID userId, CreateCommentRequest request) {
        Task task = taskService.findActiveTask(taskId);
        projectAuthorizationService.requireCanComment(task.getProjectId(), userId);

        Comment comment = new Comment();
        comment.setTaskId(taskId);
        comment.setAuthorId(userId);
        comment.setBody(parseMentions(request.body()));
        return CommentMapper.toResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listForTask(UUID taskId, UUID userId) {
        Task task = taskService.findActiveTask(taskId);
        projectAuthorizationService.requireVisibleProject(task.getProjectId(), userId);
        return commentRepository.findByTaskIdAndDeletedAtIsNullOrderByCreatedAtAsc(taskId).stream()
                .map(CommentMapper::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse update(UUID commentId, UUID userId, UpdateCommentRequest request) {
        Comment comment = findActiveComment(commentId);
        Task task = taskService.findActiveTask(comment.getTaskId());
        requireAuthorOrManager(comment, task.getProjectId(), userId);

        comment.setBody(parseMentions(request.body()));
        if (Duration.between(comment.getCreatedAt(), Instant.now()).compareTo(EDIT_WINDOW) > 0) {
            comment.setEditedAt(Instant.now());
        }

        return CommentMapper.toResponse(commentRepository.save(comment));
    }

    @Transactional
    public void delete(UUID commentId, UUID userId) {
        Comment comment = findActiveComment(commentId);
        Task task = taskService.findActiveTask(comment.getTaskId());
        requireAuthorOrManager(comment, task.getProjectId(), userId);
        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
    }

    public Comment findActiveComment(UUID commentId) {
        return commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private void requireAuthorOrManager(Comment comment, UUID projectId, UUID userId) {
        if (comment.getAuthorId().equals(userId)) {
            return;
        }
        try {
            projectAuthorizationService.requireProjectManager(projectId, userId);
        } catch (ForbiddenException ex) {
            throw new ForbiddenException("Insufficient permissions for this operation");
        }
    }

    private String parseMentions(String body) {
        MENTION_PATTERN.matcher(body);
        return body;
    }
}
