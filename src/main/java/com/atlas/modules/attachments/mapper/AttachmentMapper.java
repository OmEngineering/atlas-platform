package com.atlas.modules.attachments.mapper;

import com.atlas.modules.attachments.dto.AttachmentResponse;
import com.atlas.modules.attachments.entity.Attachment;

public final class AttachmentMapper {

    private AttachmentMapper() {
    }

    public static AttachmentResponse toResponse(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getTaskId(),
                attachment.getCommentId(),
                attachment.getUploadedBy(),
                attachment.getFileName(),
                attachment.getMimeType(),
                attachment.getSizeBytes(),
                attachment.getScanStatus().name(),
                attachment.getCreatedAt()
        );
    }
}
