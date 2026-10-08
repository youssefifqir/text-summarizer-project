package com.youssef.textsummarizerbackendms1.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(String message) {
    }

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final String maxFileSize;

    public ApiExceptionHandler(@Value("${spring.servlet.multipart.max-file-size:10MB}") String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ApiError> handleDocument(DocumentProcessingException e) {
        return ResponseEntity.status(e.getStatus()).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(HuggingFaceException.class)
    public ResponseEntity<ApiError> handleHuggingFace(HuggingFaceException e) {
        log.warn("Hugging Face call failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("The summarization model could not be reached. " + e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError("This file is too large. The limit is " + maxFileSize + "."));
    }

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest().body(new ApiError("Invalid request: " + e.getMessage()));
    }
}
