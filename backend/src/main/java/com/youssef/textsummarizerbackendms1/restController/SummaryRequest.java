package com.youssef.textsummarizerbackendms1.restController;

public record SummaryRequest(String text) {
    public SummaryRequest {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }
    }
}
