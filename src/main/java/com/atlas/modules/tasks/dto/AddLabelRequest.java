package com.atlas.modules.tasks.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddLabelRequest(@NotNull UUID labelId) {
}
