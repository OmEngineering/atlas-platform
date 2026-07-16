package com.atlas.modules.tasks.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddAssigneeRequest(@NotNull UUID userId) {
}
