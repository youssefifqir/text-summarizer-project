package com.youssef.textsummarizerbackendms1.service.document;

import com.youssef.textsummarizerbackendms1.exception.DocumentProcessingException;
import com.youssef.textsummarizerbackendms1.model.ExtractedDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Reads the text out of an uploaded PDF, DOCX or plain text file. */
@Component
public class DocumentTextExtractor {

    public ExtractedDocument extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentProcessingException(HttpStatus.BAD_REQUEST, "The uploaded file is empty.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new DocumentProcessingException(HttpStatus.BAD_REQUEST, "The file could not be read.", e);
        }

        ExtractedDocument document = switch (extension) {
            case "pdf" -> readPdf(bytes);
            case "docx" -> readDocx(bytes);
            case "txt" -> new ExtractedDocument(stripBom(new String(bytes, StandardCharsets.UTF_8)), "txt", null);
            case "doc" -> throw new DocumentProcessingException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Old .doc files are not supported. Save the file as .docx and upload it again.");
            default -> throw new DocumentProcessingException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file type. Upload a PDF, DOCX or TXT file.");
        };

        if (document.text().isBlank()) {
            throw new DocumentProcessingException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No readable text was found in this document. Scanned pages are images, and this app does not read images.");
        }
        return document;
    }

    private ExtractedDocument readPdf(byte[] bytes) {
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(pdf);
            return new ExtractedDocument(text, "pdf", pdf.getNumberOfPages());
        } catch (IOException e) {
            throw new DocumentProcessingException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "This PDF could not be read. It may be corrupted or password protected.", e);
        }
    }

    private ExtractedDocument readDocx(byte[] bytes) {
        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(docx)) {
            return new ExtractedDocument(extractor.getText(), "docx", null);
        } catch (IOException | RuntimeException e) {
            throw new DocumentProcessingException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "This DOCX file could not be read. It may be corrupted.", e);
        }
    }

    private static String extensionOf(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private static String stripBom(String text) {
        return text.startsWith("﻿") ? text.substring(1) : text;
    }
}
