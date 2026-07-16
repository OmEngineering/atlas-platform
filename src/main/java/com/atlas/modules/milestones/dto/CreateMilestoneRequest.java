package com.atlas.modules.milestones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateMilestoneRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        LocalDate startDate,
        LocalDate dueDate,
        String status
) {
}
