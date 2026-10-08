package com.youssef.textsummarizerbackendms1.service.facade;

import org.springframework.stereotype.Service;

@Service
public interface TextSummarizerService {
    String summarize(String text);
}
