package com.youssef.textsummarizerbackendms1.service.impl;

import com.youssef.textsummarizerbackendms1.exception.DocumentProcessingException;
import com.youssef.textsummarizerbackendms1.model.DocumentSummary;
import com.youssef.textsummarizerbackendms1.model.ExtractedDocument;
import com.youssef.textsummarizerbackendms1.service.document.DocumentTextExtractor;
import com.youssef.textsummarizerbackendms1.service.document.TextChunker;
import com.youssef.textsummarizerbackendms1.service.facade.DocumentSummarizerService;
import com.youssef.textsummarizerbackendms1.service.facade.TextSummarizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Summarizes a whole document with a model that only reads a few pages at a time:
 * extract the text, cut it into pieces, summarize each piece, then summarize the
 * piece summaries until one summary is left.
 */
@Service
public class DocumentSummarizerServiceImpl implements DocumentSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSummarizerServiceImpl.class);
    private static final int MAX_REDUCE_PASSES = 3;

    private final DocumentTextExtractor extractor;
    private final TextChunker chunker;
    private final TextSummarizerService textSummarizer;
    private final int chunkSize;
    private final int maxChunks;

    public DocumentSummarizerServiceImpl(DocumentTextExtractor extractor,
                                         TextChunker chunker,
                                         TextSummarizerService textSummarizer,
                                         @Value("${summarizer.chunk-size:3000}") int chunkSize,
                                         @Value("${summarizer.max-chunks:40}") int maxChunks) {
        this.extractor = extractor;
        this.chunker = chunker;
        this.textSummarizer = textSummarizer;
        this.chunkSize = chunkSize;
        this.maxChunks = maxChunks;
    }

    @Override
    public DocumentSummary summarize(MultipartFile file) {
        ExtractedDocument document = extractor.extract(file);
        List<String> chunks = chunker.chunk(document.text(), chunkSize);

        if (chunks.size() > maxChunks) {
            throw new DocumentProcessingException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "This document is too long to summarize (" + chunks.size() + " sections, the limit is " + maxChunks + ").");
        }

        log.info("Summarizing {} ({} characters, {} sections)", file.getOriginalFilename(), document.text().length(), chunks.size());

        List<String> sectionSummaries = chunks.stream().map(textSummarizer::summarize).toList();
        String summary = reduce(sectionSummaries);

        return new DocumentSummary(
                displayName(file.getOriginalFilename()),
                document.fileType(),
                document.pages(),
                document.text().length(),
                chunks.size(),
                summary,
                chunks.size() > 1 ? sectionSummaries : List.of());
    }

    /** Summarizes the section summaries together, repeating while they are still too long for one model call. */
    private String reduce(List<String> summaries) {
        List<String> current = summaries;
        String combined = String.join(" ", current);
        int passes = 0;
        while (current.size() > 1 && passes < MAX_REDUCE_PASSES) {
            current = chunker.chunk(combined, chunkSize).stream().map(textSummarizer::summarize).toList();
            combined = String.join(" ", current);
            passes++;
        }
        return combined;
    }

    private static String displayName(String originalFilename) {
        String name = StringUtils.getFilename(StringUtils.cleanPath(originalFilename == null ? "" : originalFilename));
        return name == null || name.isBlank() ? "document" : name;
    }
}
