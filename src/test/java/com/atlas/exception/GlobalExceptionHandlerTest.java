package com.atlas.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void mapsApiExceptionToDocumentedErrorShape() {
        ResponseEntity<ErrorResponse> response = handler.handleApiException(
                new ResourceNotFoundException("Project not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().error().message()).isEqualTo("Project not found");
    }
}
