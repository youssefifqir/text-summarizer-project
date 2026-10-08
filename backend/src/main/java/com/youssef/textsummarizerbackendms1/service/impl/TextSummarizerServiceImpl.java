package com.youssef.textsummarizerbackendms1.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youssef.textsummarizerbackendms1.exception.HuggingFaceException;
import com.youssef.textsummarizerbackendms1.service.facade.TextSummarizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
public class TextSummarizerServiceImpl implements TextSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(TextSummarizerServiceImpl.class);
    private static final int ERROR_BODY_MAX_CHARS = 300;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String hfToken;
    private final String modelPath;
    private final Duration timeout;

    public TextSummarizerServiceImpl(WebClient.Builder webClientBuilder,
                                     ObjectMapper objectMapper,
                                     @Value("${huggingface.api.base-url}") String baseUrl,
                                     @Value("${huggingface.api.token}") String hfToken,
                                     @Value("${huggingface.model.url}") String modelPath,
                                     @Value("${huggingface.api.timeout-seconds:90}") long timeoutSeconds) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.hfToken = hfToken;
        this.modelPath = modelPath;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public String summarize(String text) {
        if (hfToken == null || hfToken.isBlank()) {
            throw new HuggingFaceException("No Hugging Face token configured. Set the HF_API_TOKEN environment variable.");
        }

        log.debug("Sending {} characters to Hugging Face model {}", text.length(), modelPath);

        String responseJson = webClient.post()
                .uri("/" + modelPath)
                .headers(headers -> {
                    headers.setBearerAuth(hfToken);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(Map.of("inputs", text))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new HuggingFaceException(
                                        "Hugging Face returned " + response.statusCode().value() + ": " + abbreviate(body)))))
                .bodyToMono(String.class)
                .block(timeout);

        return extractSummary(responseJson);
    }

    private String extractSummary(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode first = root.isArray() ? root.get(0) : root;
            if (first != null && first.hasNonNull("summary_text")) {
                return first.get("summary_text").asText();
            }
        } catch (Exception e) {
            throw new HuggingFaceException("Could not read the Hugging Face response: " + abbreviate(responseJson), e);
        }
        throw new HuggingFaceException("Unexpected Hugging Face response: " + abbreviate(responseJson));
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= ERROR_BODY_MAX_CHARS ? value : value.substring(0, ERROR_BODY_MAX_CHARS) + "...";
    }
}
