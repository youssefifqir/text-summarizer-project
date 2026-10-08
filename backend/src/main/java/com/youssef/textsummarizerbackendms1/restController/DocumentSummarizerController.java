package com.youssef.textsummarizerbackendms1.restController;

import com.youssef.textsummarizerbackendms1.model.DocumentSummary;
import com.youssef.textsummarizerbackendms1.model.ExportRequest;
import com.youssef.textsummarizerbackendms1.service.document.SummaryDocumentExporter;
import com.youssef.textsummarizerbackendms1.service.facade.DocumentSummarizerService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/documents")
public class DocumentSummarizerController {

    private static final MediaType DOCX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final DocumentSummarizerService documentSummarizerService;
    private final SummaryDocumentExporter exporter;

    public DocumentSummarizerController(DocumentSummarizerService documentSummarizerService,
                                        SummaryDocumentExporter exporter) {
        this.documentSummarizerService = documentSummarizerService;
        this.exporter = exporter;
    }

    @PostMapping(value = "/summarize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentSummary> summarize(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentSummarizerService.summarize(file));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody ExportRequest request) {
        String baseName = request.fileName() == null ? "document" : request.fileName().replaceFirst("\\.[^.]+$", "");
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(baseName + "-summary.docx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(DOCX)
                .body(exporter.export(request));
    }
}
