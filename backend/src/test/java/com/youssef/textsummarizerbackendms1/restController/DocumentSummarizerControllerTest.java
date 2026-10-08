package com.youssef.textsummarizerbackendms1.restController;

import com.youssef.textsummarizerbackendms1.exception.DocumentProcessingException;
import com.youssef.textsummarizerbackendms1.exception.HuggingFaceException;
import com.youssef.textsummarizerbackendms1.model.DocumentSummary;
import com.youssef.textsummarizerbackendms1.service.document.SummaryDocumentExporter;
import com.youssef.textsummarizerbackendms1.service.facade.DocumentSummarizerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentSummarizerController.class)
class DocumentSummarizerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentSummarizerService documentSummarizerService;

    @MockBean
    private SummaryDocumentExporter exporter;

    private static MockMultipartFile upload() {
        return new MockMultipartFile("file", "note.txt", "text/plain", "Some text.".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void summarizeReturnsTheSummaryAsJson() throws Exception {
        when(documentSummarizerService.summarize(any())).thenReturn(
                new DocumentSummary("note.txt", "txt", null, 10, 1, "A summary.", List.of()));

        mockMvc.perform(multipart("/api/documents/summarize").file(upload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("A summary."))
                .andExpect(jsonPath("$.fileName").value("note.txt"))
                .andExpect(jsonPath("$.sections").value(1));
    }

    @Test
    void anUnsupportedFileGives415WithAMessage() throws Exception {
        when(documentSummarizerService.summarize(any())).thenThrow(
                new DocumentProcessingException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type."));

        mockMvc.perform(multipart("/api/documents/summarize").file(upload()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Unsupported file type."));
    }

    @Test
    void aModelFailureGives502() throws Exception {
        when(documentSummarizerService.summarize(any())).thenThrow(new HuggingFaceException("Hugging Face returned 503"));

        mockMvc.perform(multipart("/api/documents/summarize").file(upload()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("503")));
    }

    @Test
    void aMissingFilePartGives400() throws Exception {
        mockMvc.perform(multipart("/api/documents/summarize"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportReturnsAWordDocumentNamedAfterTheSource() throws Exception {
        when(exporter.export(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(post("/api/documents/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"report.pdf\",\"summary\":\"A summary.\",\"sectionSummaries\":[]}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("report-summary.docx")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void exportRejectsAnEmptySummary() throws Exception {
        mockMvc.perform(post("/api/documents/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"report.pdf\",\"summary\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
