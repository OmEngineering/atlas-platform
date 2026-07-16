package com.atlas.modules.attachments.controller;

import com.atlas.modules.attachments.dto.AttachmentResponse;
import com.atlas.modules.attachments.entity.Attachment;
import com.atlas.modules.attachments.mapper.AttachmentMapper;
import com.atlas.modules.attachments.service.AttachmentService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@Tag(name = "Attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final SecurityContextAccessor securityContextAccessor;

    public AttachmentController(
            AttachmentService attachmentService,
            SecurityContextAccessor securityContextAccessor) {
        this.attachmentService = attachmentService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PostMapping("/api/v1/tasks/{taskId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AttachmentResponse> uploadForTask(
            @PathVariable UUID taskId,
            @RequestParam("file") MultipartFile file) throws IOException {
        Attachment attachment = attachmentService.uploadForTask(
                taskId, securityContextAccessor.currentUserId(), file);
        return ApiResponse.of(AttachmentMapper.toResponse(attachment));
    }

    @PostMapping("/api/v1/comments/{commentId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AttachmentResponse> uploadForComment(
            @PathVariable UUID commentId,
            @RequestParam("file") MultipartFile file) throws IOException {
        Attachment attachment = attachmentService.uploadForComment(
                commentId, securityContextAccessor.currentUserId(), file);
        return ApiResponse.of(AttachmentMapper.toResponse(attachment));
    }

    @GetMapping("/api/v1/attachments/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable UUID id) {
        AttachmentService.DownloadResult result = attachmentService.download(
                id, securityContextAccessor.currentUserId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .body(result.resource());
    }
}
