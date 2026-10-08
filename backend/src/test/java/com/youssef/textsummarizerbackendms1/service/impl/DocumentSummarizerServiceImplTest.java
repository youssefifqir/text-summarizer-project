package com.youssef.textsummarizerbackendms1.service.impl;

import com.youssef.textsummarizerbackendms1.exception.DocumentProcessingException;
import com.youssef.textsummarizerbackendms1.model.DocumentSummary;
import com.youssef.textsummarizerbackendms1.service.document.DocumentTextExtractor;
import com.youssef.textsummarizerbackendms1.service.document.TextChunker;
import com.youssef.textsummarizerbackendms1.service.facade.TextSummarizerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentSummarizerServiceImplTest {

    private static final int CHUNK_SIZE = 300;

    private TextSummarizerService textSummarizer;
    private DocumentSummarizerServiceImpl service;

    @BeforeEach
    void setUp() {
        textSummarizer = mock(TextSummarizerService.class);
        service = new DocumentSummarizerServiceImpl(new DocumentTextExtractor(), new TextChunker(), textSummarizer, CHUNK_SIZE, 5);
    }

    private static MockMultipartFile textFile(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    /** About {@code sentences} sentences of 60 characters each. */
    private static String sentences(int sentences) {
        List<String> all = new ArrayList<>();
        for (int i = 0; i < sentences; i++) {
            all.add("This is sentence number " + String.format("%02d", i) + " and it is quite ordinary.");
        }
        return String.join(" ", all);
    }

    @Test
    void aShortDocumentIsSummarizedInOneCall() {
        when(textSummarizer.summarize(anyString())).thenReturn("The summary.");

        DocumentSummary result = service.summarize(textFile("note.txt", sentences(3)));

        assertThat(result.summary()).isEqualTo("The summary.");
        assertThat(result.sections()).isEqualTo(1);
        assertThat(result.sectionSummaries()).isEmpty();
        assertThat(result.fileName()).isEqualTo("note.txt");
        assertThat(result.fileType()).isEqualTo("txt");
        verify(textSummarizer, times(1)).summarize(anyString());
    }

    @Test
    void aLongDocumentIsSummarizedPerSectionThenMerged() {
        when(textSummarizer.summarize(anyString())).thenReturn("partial");

        DocumentSummary result = service.summarize(textFile("long.txt", sentences(20)));

        // 20 sentences of about 60 characters need several 300 character sections.
        assertThat(result.sections()).isGreaterThan(2);
        assertThat(result.sectionSummaries()).hasSize(result.sections()).containsOnly("partial");
        // one call per section, then one call to merge the short section summaries
        verify(textSummarizer, times(result.sections() + 1)).summarize(anyString());
        assertThat(result.summary()).isEqualTo("partial");
    }

    @Test
    void theMergeStepReceivesTheSectionSummaries() {
        // Sections start with "This is sentence", the merge input starts with the section summaries.
        when(textSummarizer.summarize(anyString()))
                .thenAnswer(call -> call.getArgument(0, String.class).startsWith("This") ? "part" : "merged");

        DocumentSummary result = service.summarize(textFile("long.txt", sentences(12)));

        assertThat(result.summary()).isEqualTo("merged");
        assertThat(result.sectionSummaries()).containsOnly("part");
        verify(textSummarizer).summarize(String.join(" ", result.sectionSummaries()));
    }

    @Test
    void refusesADocumentWithTooManySections() {
        assertThatThrownBy(() -> service.summarize(textFile("huge.txt", sentences(100))))
                .isInstanceOfSatisfying(DocumentProcessingException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
                    assertThat(e.getMessage()).contains("too long");
                });
        verify(textSummarizer, never()).summarize(anyString());
    }

    @Test
    void stripsAnyFolderFromTheReportedFileName() {
        when(textSummarizer.summarize(anyString())).thenReturn("The summary.");

        DocumentSummary result = service.summarize(textFile("C:\\Users\\someone\\docs\\note.txt", sentences(2)));

        assertThat(result.fileName()).isEqualTo("note.txt");
    }
}
