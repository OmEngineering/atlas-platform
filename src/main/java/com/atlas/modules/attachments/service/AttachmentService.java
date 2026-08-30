package com.atlas.modules.attachments.service;

import com.atlas.exception.ForbiddenException;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.attachments.entity.Attachment;
import com.atlas.modules.attachments.entity.AttachmentScanStatus;
import com.atlas.modules.attachments.repository.AttachmentRepository;
import com.atlas.modules.comments.entity.Comment;
import com.atlas.modules.comments.service.CommentService;
import com.atlas.modules.projects.service.ProjectAuthorizationService;
import com.atlas.modules.tasks.entity.Task;
import com.atlas.modules.tasks.service.TaskService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskService taskService;
    private final CommentService commentService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final FileStorageService fileStorageService;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            TaskService taskService,
            CommentService commentService,
            ProjectAuthorizationService projectAuthorizationService,
            FileStorageService fileStorageService) {
        this.attachmentRepository = attachmentRepository;
        this.taskService = taskService;
        this.commentService = commentService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public Attachment uploadForTask(UUID taskId, UUID userId, MultipartFile file) throws IOException {
        Task task = taskService.findActiveTask(taskId);
        projectAuthorizationService.requireCanComment(task.getProjectId(), userId);
        return saveAttachment(userId, file, taskId, null);
    }

    @Transactional
    public Attachment uploadForComment(UUID commentId, UUID userId, MultipartFile file) throws IOException {
        Comment comment = commentService.findActiveComment(commentId);
        Task task = taskService.findActiveTask(comment.getTaskId());
        projectAuthorizationService.requireCanComment(task.getProjectId(), userId);
        return saveAttachment(userId, file, null, commentId);
    }

    @Transactional(readOnly = true)
    public DownloadResult download(UUID attachmentId, UUID userId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        UUID projectId = resolveProjectId(attachment);
        projectAuthorizationService.requireVisibleProject(projectId, userId);

        if (attachment.getScanStatus() != AttachmentScanStatus.CLEAN) {
            throw new ForbiddenException("Attachment is not available for download");
        }

        Resource resource = fileStorageService.loadAsResource(attachment.getStorageKey());
        return new DownloadResult(resource, attachment.getFileName(), attachment.getMimeType());
    }

    private Attachment saveAttachment(UUID userId, MultipartFile file, UUID taskId, UUID commentId)
            throws IOException {
        String storageKey = fileStorageService.store(file);

        Attachment attachment = new Attachment();
        attachment.setTaskId(taskId);
        attachment.setCommentId(commentId);
        attachment.setUploadedBy(userId);
        attachment.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        attachment.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        attachment.setSizeBytes(file.getSize());
        attachment.setStorageKey(storageKey);
        attachment.setScanStatus(AttachmentScanStatus.CLEAN);
        return attachmentRepository.save(attachment);
    }

    private UUID resolveProjectId(Attachment attachment) {
        if (attachment.getTaskId() != null) {
            return taskService.findActiveTask(attachment.getTaskId()).getProjectId();
        }
        Comment comment = commentService.findActiveComment(attachment.getCommentId());
        return taskService.findActiveTask(comment.getTaskId()).getProjectId();
    }

    public record DownloadResult(Resource resource, String fileName, String mimeType) {
    }
}
