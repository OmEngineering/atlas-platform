package com.atlas.modules.tasks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank @Size(max = 500) String title,
        String description,
        String status,
        String priority,
        LocalDate dueDate,
        UUID milestoneId
) {
}
