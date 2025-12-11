package com.example.orderorchestrator.adapter.in.web;

import com.example.orderorchestrator.adapter.in.web.dto.ApiError;
import com.example.orderorchestrator.adapter.in.web.dto.response.ApiResponse;
import com.example.orderorchestrator.domain.exception.AccountNotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

// 전역 예외 처리기 (ApiResponse 패턴 적용 - 분리된 핸들러)
@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. AccountNotFoundException (404 Not Found 관련) 처리
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(AccountNotFoundException ex) {
        return responseEntityWithHttpStatus(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    // 2. IllegalArgumentException (400 Bad Request 관련) 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    // 3. IllegalStateException (409 Conflict 관련) 처리
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(IllegalStateException ex) {
        return responseEntityWithHttpStatus(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }


     /* 최종 fallback: 잡히지 않은 모든 Exception (500 Internal Server Error) 처리 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleServerError(Exception ex) {
        return responseEntityWithHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "Internal Server Error occurred.");
    }

    private static ResponseEntity<ApiResponse<Object>> responseEntityWithHttpStatus(HttpStatus notFound, String NOT_FOUND, String ex) {
        return ResponseEntity
                .status(notFound)      // 🔹 404
                .body(ApiResponse.failure(ApiError.of(NOT_FOUND, ex)));
    }

}
