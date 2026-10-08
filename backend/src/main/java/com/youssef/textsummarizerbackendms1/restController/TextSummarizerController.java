package com.youssef.textsummarizerbackendms1.restController;
import com.youssef.textsummarizerbackendms1.service.facade.TextSummarizerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summarize")
public class TextSummarizerController {

   private final TextSummarizerService textSummarizerService;

    public TextSummarizerController(TextSummarizerService textSummarizerService) {
        this.textSummarizerService = textSummarizerService;
    }

    @PostMapping
    public ResponseEntity<String> summarize(@RequestBody String text) {
        try {
            return ResponseEntity.ok(textSummarizerService.summarize(text));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Summarization failed: " + e.getMessage());
        }
    }
}