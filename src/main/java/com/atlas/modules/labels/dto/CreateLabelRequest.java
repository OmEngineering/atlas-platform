package com.atlas.modules.labels.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLabelRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex value like #RRGGBB")
        String color
) {
}
