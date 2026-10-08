package com.youssef.textsummarizerbackendms1.model;

import java.util.List;

/** What the client sends back to download a summary as a Word document. */
public record ExportRequest(String fileName,
                            Integer pages,
                            Integer characters,
                            String summary,
                            List<String> sectionSummaries) {

    public ExportRequest {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Summary cannot be empty");
        }
        sectionSummaries = sectionSummaries == null ? List.of() : List.copyOf(sectionSummaries);
    }
}
