package com.atlas.modules.milestones.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MilestoneResponse(
        UUID id,
        UUID projectId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate dueDate,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
