package com.atlas.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorDetail(
        String code,
        String message,
        Map<String, Object> details
) {
}
