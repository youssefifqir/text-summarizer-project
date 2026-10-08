package com.youssef.textsummarizerbackendms1.service.document;

import com.youssef.textsummarizerbackendms1.model.ExportRequest;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryDocumentExporterTest {

    private final SummaryDocumentExporter exporter = new SummaryDocumentExporter();

    @Test
    void buildsAStructuredDocumentWithSectionBlocks() throws Exception {
        byte[] docx = exporter.export(new ExportRequest("contract.pdf", 12, 40500,
                "The contract sets monthly rent and a two year term.",
                List.of("First part: rent and term.", "Second part: termination rules.")));

        String text = textOf(docx);

        assertThat(text).contains("Document summary", "Source: contract.pdf", "Pages: 12", "Length: 40500 characters");
        assertThat(text).contains("Summary", "The contract sets monthly rent and a two year term.");
        assertThat(text).contains("Summary by section", "Section 1", "First part: rent and term.",
                "Section 2", "Second part: termination rules.");
    }

    @Test
    void leavesOutTheSectionBlockForASinglePieceDocument() throws Exception {
        byte[] docx = exporter.export(new ExportRequest("note.txt", null, null, "A short summary.", List.of()));

        String text = textOf(docx);

        assertThat(text).contains("Source: note.txt", "A short summary.");
        assertThat(text).doesNotContain("Summary by section", "Pages:");
    }

    private static String textOf(byte[] docx) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
