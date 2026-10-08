package com.youssef.textsummarizerbackendms1.service.document;

import com.youssef.textsummarizerbackendms1.exception.DocumentProcessingException;
import com.youssef.textsummarizerbackendms1.model.ExtractedDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTextExtractorTest {

    private final DocumentTextExtractor extractor = new DocumentTextExtractor();

    @Test
    void readsATextFile() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain",
                "﻿Hello from a text file.".getBytes(StandardCharsets.UTF_8));

        ExtractedDocument document = extractor.extract(file);

        assertThat(document.text()).isEqualTo("Hello from a text file.");
        assertThat(document.fileType()).isEqualTo("txt");
        assertThat(document.pages()).isNull();
    }

    @Test
    void readsTextAndPageCountFromAPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "report.PDF", "application/pdf",
                pdfWithPages("Quarterly revenue grew by twelve percent.", "Second page about costs."));

        ExtractedDocument document = extractor.extract(file);

        assertThat(document.text()).contains("Quarterly revenue grew by twelve percent.", "Second page about costs.");
        assertThat(document.fileType()).isEqualTo("pdf");
        assertThat(document.pages()).isEqualTo(2);
    }

    @Test
    void readsParagraphsFromADocx() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "contract.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxWithParagraphs("The tenant pays rent monthly.", "The landlord keeps the building safe."));

        ExtractedDocument document = extractor.extract(file);

        assertThat(document.text()).contains("The tenant pays rent monthly.", "The landlord keeps the building safe.");
        assertThat(document.fileType()).isEqualTo("docx");
    }

    @Test
    void rejectsAnUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "program.exe", "application/octet-stream", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOfSatisfying(DocumentProcessingException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void askForDocxWhenGivenTheOldDocFormat() {
        MockMultipartFile file = new MockMultipartFile("file", "old.doc", "application/msword", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOfSatisfying(DocumentProcessingException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                    assertThat(e.getMessage()).contains(".docx");
                });
    }

    @Test
    void rejectsAnEmptyUpload() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOfSatisfying(DocumentProcessingException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void explainsWhyAPdfWithoutTextCannotBeSummarized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "scan.pdf", "application/pdf", pdfWithPages(""));

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOfSatisfying(DocumentProcessingException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(e.getMessage()).contains("No readable text");
                });
    }

    @Test
    void reportsACorruptPdf() {
        MockMultipartFile file = new MockMultipartFile("file", "broken.pdf", "application/pdf",
                "this is not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOfSatisfying(DocumentProcessingException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void reportsACorruptDocx() {
        MockMultipartFile file = new MockMultipartFile("file", "broken.docx", "application/octet-stream",
                "this is not a docx".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOfSatisfying(DocumentProcessingException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private static byte[] pdfWithPages(String... pageTexts) throws Exception {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                pdf.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(pageText);
                    content.endText();
                }
            }
            pdf.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] docxWithParagraphs(String... paragraphs) throws Exception {
        try (XWPFDocument docx = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String paragraph : paragraphs) {
                docx.createParagraph().createRun().setText(paragraph);
            }
            docx.write(out);
            return out.toByteArray();
        }
    }
}
