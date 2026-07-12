package com.atlas.modules.projects.dto;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 255) String name,
        String description,
        String visibility
) {
}
