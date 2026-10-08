package com.youssef.textsummarizerbackendms1.model;

/** Plain text pulled out of an uploaded file. {@code pages} is null for formats without pages. */
public record ExtractedDocument(String text, String fileType, Integer pages) {
}
