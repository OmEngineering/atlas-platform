package com.atlas.modules.tasks.service;

import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.attachments.repository.AttachmentRepository;
import com.atlas.modules.comments.repository.CommentRepository;
import com.atlas.modules.labels.entity.Label;
import com.atlas.modules.labels.service.LabelService;
import com.atlas.modules.milestones.repository.MilestoneRepository;
import com.atlas.modules.projects.entity.Project;
import com.atlas.modules.projects.repository.ProjectMembershipRepository;
import com.atlas.modules.projects.repository.ProjectRepository;
import com.atlas.modules.projects.service.ProjectAuthorizationService;
import com.atlas.modules.tasks.dto.AddAssigneeRequest;
import com.atlas.modules.tasks.dto.AddLabelRequest;
import com.atlas.modules.tasks.dto.CreateTaskRequest;
import com.atlas.modules.tasks.dto.TaskResponse;
import com.atlas.modules.tasks.dto.UpdateTaskRequest;
import com.atlas.modules.tasks.entity.Task;
import com.atlas.modules.tasks.entity.TaskPriority;
import com.atlas.modules.tasks.entity.TaskStatus;
import com.atlas.modules.tasks.mapper.TaskMapper;
import com.atlas.modules.tasks.repository.TaskRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final MilestoneRepository milestoneRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final LabelService labelService;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            ProjectMembershipRepository projectMembershipRepository,
            MilestoneRepository milestoneRepository,
            CommentRepository commentRepository,
            AttachmentRepository attachmentRepository,
            ProjectAuthorizationService projectAuthorizationService,
            LabelService labelService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectMembershipRepository = projectMembershipRepository;
        this.milestoneRepository = milestoneRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.projectAuthorizationService = projectAuthorizationService;
        this.labelService = labelService;
    }

    @Transactional
    public TaskResponse create(UUID projectId, UUID userId, CreateTaskRequest request) {
        Project project = projectAuthorizationService.requireVisibleProject(projectId, userId);
        projectAuthorizationService.requireCanManageTasks(projectId, userId);

        if (request.milestoneId() != null) {
            var milestone = milestoneRepository.findById(request.milestoneId())
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.VALIDATION_ERROR, "Milestone not found", HttpStatus.UNPROCESSABLE_ENTITY));
            if (!milestone.getProjectId().equals(projectId)) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR, "Milestone not found", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        Task task = new Task();
        task.setProjectId(projectId);
        task.setMilestoneId(request.milestoneId());
        task.setKey(generateTaskKey(project));
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setStatus(parseStatus(request.status()));
        task.setPriority(parsePriority(request.priority()));
        task.setDueDate(request.dueDate());
        task.setCreatedBy(userId);
        applyStatusSideEffects(task, task.getStatus());

        task = saveWithKeyRetry(task, project);
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listForProject(
            UUID projectId,
            UUID userId,
            String status,
            UUID assigneeId,
            UUID labelId,
            UUID milestoneId) {
        projectAuthorizationService.requireVisibleProject(projectId, userId);
        TaskStatus taskStatus = status != null && !status.isBlank()
                ? parseStatus(status) : null;
        return taskRepository.findByProjectWithFilters(
                        projectId, taskStatus, assigneeId, labelId, milestoneId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID taskId, UUID userId) {
        Task task = findActiveTask(taskId);
        projectAuthorizationService.requireVisibleProject(task.getProjectId(), userId);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse update(UUID taskId, UUID userId, UpdateTaskRequest request) {
        Task task = findActiveTask(taskId);
        projectAuthorizationService.requireCanManageTasks(task.getProjectId(), userId);

        if (request.title() != null) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.milestoneId() != null) {
            var milestone = milestoneRepository.findById(request.milestoneId())
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.VALIDATION_ERROR, "Milestone not found", HttpStatus.UNPROCESSABLE_ENTITY));
            if (!milestone.getProjectId().equals(task.getProjectId())) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR, "Milestone not found", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            task.setMilestoneId(request.milestoneId());
        }
        if (request.priority() != null) {
            task.setPriority(parsePriority(request.priority()));
        }
        if (request.status() != null) {
            TaskStatus newStatus = parseStatus(request.status());
            applyStatusSideEffects(task, newStatus);
            task.setStatus(newStatus);
        }

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(UUID taskId, UUID userId) {
        Task task = findActiveTask(taskId);
        projectAuthorizationService.requireCanManageTasks(task.getProjectId(), userId);
        task.setDeletedAt(Instant.now());
        taskRepository.save(task);
    }

    @Transactional
    public TaskResponse addAssignee(UUID taskId, UUID userId, AddAssigneeRequest request) {
        Task task = findActiveTask(taskId);
        projectAuthorizationService.requireCanManageTasks(task.getProjectId(), userId);

        if (!projectMembershipRepository.existsByProjectIdAndUserId(task.getProjectId(), request.userId())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "User is not a member of this project",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        task.getAssigneeIds().add(request.userId());
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse removeAssignee(UUID taskId, UUID assigneeId, UUID userId) {
        Task task = findActiveTask(taskId);
        projectAuthorizationService.requireCanManageTasks(task.getProjectId(), userId);
        task.getAssigneeIds().remove(assigneeId);
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse addLabel(UUID taskId, UUID userId, AddLabelRequest request) {
        Task task = findActiveTask(taskId);
        Project project = projectAuthorizationService.requireVisibleProject(task.getProjectId(), userId);
        projectAuthorizationService.requireCanManageTasks(task.getProjectId(), userId);

        Label label = labelService.findUsableLabel(request.labelId(), project.getOrganizationId(), task.getProjectId());
        task.getLabelIds().add(label.getId());
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse removeLabel(UUID taskId, UUID labelId, UUID userId) {
        Task task = findActiveTask(taskId);
        projectAuthorizationService.requireCanManageTasks(task.getProjectId(), userId);
        task.getLabelIds().remove(labelId);
        return toResponse(taskRepository.save(task));
    }

    public Task findActiveTask(UUID taskId) {
        return taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private TaskResponse toResponse(Task task) {
        long commentsCount = commentRepository.countByTaskIdAndDeletedAtIsNull(task.getId());
        long attachmentsCount = attachmentRepository.countByTaskId(task.getId());
        return TaskMapper.toResponse(task, commentsCount, attachmentsCount);
    }

    private String generateTaskKey(Project project) {
        long count = taskRepository.countByProjectId(project.getId());
        return project.getKey() + "-" + (count + 1);
    }

    private Task saveWithKeyRetry(Task task, Project project) {
        try {
            return taskRepository.save(task);
        } catch (DataIntegrityViolationException ex) {
            task.setKey(generateTaskKey(project));
            return taskRepository.save(task);
        }
    }

    private void applyStatusSideEffects(Task task, TaskStatus status) {
        if (status == TaskStatus.DONE) {
            task.setCompletedAt(Instant.now());
        } else {
            task.setCompletedAt(null);
        }
    }

    private TaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return TaskStatus.TODO;
        }
        try {
            return TaskStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid task status", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private TaskPriority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return TaskPriority.MEDIUM;
        }
        try {
            return TaskPriority.valueOf(priority.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid task priority", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
