package com.atlas.modules.comments.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID taskId,
        UUID authorId,
        String body,
        boolean edited,
        Instant editedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
