package com.atlas.modules.tasks.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        UUID milestoneId,
        String key,
        String title,
        String description,
        String status,
        String priority,
        LocalDate dueDate,
        Instant completedAt,
        UUID createdBy,
        List<UUID> assigneeIds,
        List<UUID> labelIds,
        long commentsCount,
        long attachmentsCount,
        Instant createdAt,
        Instant updatedAt
) {
}
