package com.youssef.textsummarizerbackendms1.service.document;

import com.youssef.textsummarizerbackendms1.model.ExportRequest;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Builds a structured Word document from a summary: title, source details, overall summary, then one block per section. */
@Component
public class SummaryDocumentExporter {

    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] export(ExportRequest request) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            line(document, "Document summary", 20, true);

            if (request.fileName() != null && !request.fileName().isBlank()) {
                line(document, "Source: " + request.fileName(), 11, false);
            }
            if (request.pages() != null) {
                line(document, "Pages: " + request.pages(), 11, false);
            }
            if (request.characters() != null) {
                line(document, "Length: " + request.characters() + " characters", 11, false);
            }
            line(document, "Generated: " + LocalDateTime.now().format(GENERATED_AT), 11, false);

            heading(document, "Summary");
            line(document, request.summary(), 11, false);

            List<String> sections = request.sectionSummaries();
            if (!sections.isEmpty()) {
                heading(document, "Summary by section");
                for (int i = 0; i < sections.size(); i++) {
                    line(document, "Section " + (i + 1), 11, true);
                    line(document, sections.get(i), 11, false);
                }
            }

            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build the summary document", e);
        }
    }

    private static void heading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(240);
        run(paragraph, text, 14, true);
    }

    private static void line(XWPFDocument document, String text, int size, boolean bold) {
        run(document.createParagraph(), text, size, bold);
    }

    private static void run(XWPFParagraph paragraph, String text, int size, boolean bold) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(size);
        run.setBold(bold);
    }
}
