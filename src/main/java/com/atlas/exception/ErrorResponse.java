package com.atlas.exception;

public record ErrorResponse(ErrorDetail error) {

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(new ErrorDetail(code.name(), message, null));
    }

    public static ErrorResponse of(ErrorCode code, String message, java.util.Map<String, Object> details) {
        return new ErrorResponse(new ErrorDetail(code.name(), message, details));
    }
}
