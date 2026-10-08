package com.youssef.textsummarizerbackendms1.exception;

public class HuggingFaceException extends RuntimeException {
    public HuggingFaceException(String message) {
        super(message);
    }

    public HuggingFaceException(String message, Throwable cause) {
        super(message, cause);
    }
}