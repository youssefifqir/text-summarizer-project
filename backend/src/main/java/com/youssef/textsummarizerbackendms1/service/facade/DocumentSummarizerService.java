package com.youssef.textsummarizerbackendms1.service.facade;

import com.youssef.textsummarizerbackendms1.model.DocumentSummary;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentSummarizerService {
    DocumentSummary summarize(MultipartFile file);
}
