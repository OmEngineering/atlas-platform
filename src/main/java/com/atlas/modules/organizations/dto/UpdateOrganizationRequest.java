package com.atlas.modules.organizations.dto;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 255) String name,
        @Size(max = 2048) String logoUrl,
        @Size(max = 5000) String description,
        @Size(max = 64) String timezone,
        @Size(min = 2, max = 2) String country
) {
}
