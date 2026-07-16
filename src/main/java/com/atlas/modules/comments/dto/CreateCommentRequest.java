package com.atlas.modules.comments.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(@NotBlank String body) {
}
