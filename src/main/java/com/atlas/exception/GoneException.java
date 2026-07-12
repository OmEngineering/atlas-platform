package com.atlas.exception;

import org.springframework.http.HttpStatus;

public class GoneException extends ApiException {

    public GoneException(ErrorCode code, String message) {
        super(code, message, HttpStatus.GONE);
    }
}
