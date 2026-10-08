package com.youssef.textsummarizerbackendms1.exception;

import org.springframework.http.HttpStatus;

/** A problem with an uploaded document, carrying the HTTP status the API should answer with. */
public class DocumentProcessingException extends RuntimeException {

    private final HttpStatus status;

    public DocumentProcessingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public DocumentProcessingException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
