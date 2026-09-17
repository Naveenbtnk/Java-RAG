package com.example.rag.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST", message, Instant.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidUpload(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_DOCUMENT", exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> uploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError("DOCUMENT_TOO_LARGE", "PDF files must be 10 MB or smaller", Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpectedError(Exception exception) {
        log.error("RAG request failed", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        "RAG_REQUEST_FAILED",
                        "The assistant could not answer right now. Check the server logs and configuration.",
                        Instant.now()));
    }

    public record ApiError(String code, String message, Instant timestamp) {
    }
}
