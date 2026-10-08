package com.youssef.textsummarizerbackendms1.model;

import java.util.List;

/**
 * Result of summarizing one document.
 *
 * @param sections         how many pieces the document was cut into
 * @param sectionSummaries one summary per piece, empty when the document fit in a single piece
 */
public record DocumentSummary(String fileName,
                              String fileType,
                              Integer pages,
                              int characters,
                              int sections,
                              String summary,
                              List<String> sectionSummaries) {
}
