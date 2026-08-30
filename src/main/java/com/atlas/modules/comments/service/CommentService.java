package com.atlas.modules.comments.service;

import com.atlas.event.DomainEvent;
import com.atlas.event.DomainEventPublisher;
import com.atlas.exception.ForbiddenException;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.auth.entity.User;
import com.atlas.modules.auth.repository.UserRepository;
import com.atlas.modules.comments.dto.CommentResponse;
import com.atlas.modules.comments.dto.CreateCommentRequest;
import com.atlas.modules.comments.dto.UpdateCommentRequest;
import com.atlas.modules.comments.entity.Comment;
import com.atlas.modules.comments.mapper.CommentMapper;
import com.atlas.modules.comments.repository.CommentRepository;
import com.atlas.modules.projects.entity.Project;
import com.atlas.modules.projects.repository.ProjectRepository;
import com.atlas.modules.projects.service.ProjectAuthorizationService;
import com.atlas.modules.tasks.entity.Task;
import com.atlas.modules.tasks.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommentService {

    private static final Duration EDIT_WINDOW = Duration.ofMinutes(15);
    private static final Pattern MENTION_UUID = Pattern.compile(
            "@([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");
    private static final Pattern MENTION_EMAIL = Pattern.compile("@([\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,})");

    private final CommentRepository commentRepository;
    private final TaskService taskService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DomainEventPublisher domainEventPublisher;

    public CommentService(
            CommentRepository commentRepository,
            TaskService taskService,
            ProjectAuthorizationService projectAuthorizationService,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            DomainEventPublisher domainEventPublisher) {
        this.commentRepository = commentRepository;
        this.taskService = taskService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public CommentResponse create(UUID taskId, UUID userId, CreateCommentRequest request) {
        Task task = taskService.findActiveTask(taskId);
        projectAuthorizationService.requireCanComment(task.getProjectId(), userId);

        String body = request.body();
        Set<UUID> mentionedUserIds = resolveMentions(body);

        Comment comment = new Comment();
        comment.setTaskId(taskId);
        comment.setAuthorId(userId);
        comment.setBody(body);
        comment = commentRepository.save(comment);

        Project project = projectRepository.findByIdAndDeletedAtIsNull(task.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        Map<String, Object> payload = new HashMap<>();
        payload.put("commentId", comment.getId());
        payload.put("taskId", taskId);
        payload.put("mentionedUserIds", List.copyOf(mentionedUserIds));
        payload.put("bodyPreview", truncate(body, 180));
        domainEventPublisher.publish(DomainEvent.of(
                "comment.created", project.getOrganizationId(), userId, payload));

        return CommentMapper.toResponse(comment);
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

        comment.setBody(request.body());
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

    private Set<UUID> resolveMentions(String body) {
        Set<UUID> mentioned = new LinkedHashSet<>();
        if (body == null || body.isBlank()) {
            return mentioned;
        }

        Matcher uuidMatcher = MENTION_UUID.matcher(body);
        while (uuidMatcher.find()) {
            try {
                UUID id = UUID.fromString(uuidMatcher.group(1));
                if (userRepository.existsById(id)) {
                    mentioned.add(id);
                }
            } catch (IllegalArgumentException ignored) {
                // skip invalid uuid fragment
            }
        }

        Matcher emailMatcher = MENTION_EMAIL.matcher(body);
        while (emailMatcher.find()) {
            String email = emailMatcher.group(1).toLowerCase(Locale.ROOT);
            userRepository.findByEmailIgnoreCase(email).map(User::getId).ifPresent(mentioned::add);
        }
        return mentioned;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
