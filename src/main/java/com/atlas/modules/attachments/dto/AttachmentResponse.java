package com.atlas.modules.attachments.dto;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        UUID taskId,
        UUID commentId,
        UUID uploadedBy,
        String fileName,
        String mimeType,
        long sizeBytes,
        String scanStatus,
        Instant createdAt
) {
}
