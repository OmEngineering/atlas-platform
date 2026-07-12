package com.atlas.modules.organizations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with optional hyphens")
        @Size(max = 100) String slug,
        @NotBlank @Size(max = 64) String timezone,
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be ISO 3166-1 alpha-2") String country
) {
}
