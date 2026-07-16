package com.atlas.modules.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record ChangePlanRequest(
        @NotBlank String plan,
        @NotNull @Min(1) Integer seats
) {
}
