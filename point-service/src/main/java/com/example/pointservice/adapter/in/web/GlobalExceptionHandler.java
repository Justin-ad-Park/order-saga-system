package com.example.pointservice.adapter.in.web;

import com.example.common.api.ApiError;
import com.example.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(IllegalStateException ex) {
        return responseEntityWithHttpStatus(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleServerError(Exception ex) {
        return responseEntityWithHttpStatus(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SERVER_ERROR",
                "Internal Server Error occurred."
        );
    }

    private static ResponseEntity<ApiResponse<Object>> responseEntityWithHttpStatus(
            HttpStatus status,
            String code,
            String message
    ) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.failure(ApiError.of(code, message)));
    }
}
