package com.atlas.modules.projects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9]{1,19}$", message = "Key must be 2-20 uppercase letters or digits")
        String key,
        String description,
        String visibility
) {
}
