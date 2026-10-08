package com.youssef.textsummarizerbackendms1.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.youssef.textsummarizerbackendms1.exception.HuggingFaceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs the service against a local stand-in for the Hugging Face API. */
class TextSummarizerServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private volatile int responseStatus;
    private volatile String responseBody;

    @BeforeEach
    void startServer() throws IOException {
        responseStatus = 200;
        responseBody = "[{\"summary_text\":\"A short summary.\"}]";
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private TextSummarizerServiceImpl service(String token) {
        String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/hf-inference/models";
        return new TextSummarizerServiceImpl(WebClient.builder(), objectMapper, baseUrl, token, "facebook/bart-large-cnn", 5);
    }

    @Test
    void returnsTheSummaryTextFromTheModelResponse() {
        String summary = service("test-token").summarize("A long text to summarize.");

        assertThat(summary).isEqualTo("A short summary.");
        assertThat(lastPath.get()).isEqualTo("/hf-inference/models/facebook/bart-large-cnn");
        assertThat(lastAuthorization.get()).isEqualTo("Bearer test-token");
    }

    @Test
    void sendsQuotesNewlinesAndBackslashesAsValidJson() throws Exception {
        String tricky = "He said \"stop\".\nNext line with a backslash \\ and a tab\tinside.";

        service("test-token").summarize(tricky);

        JsonNode sent = objectMapper.readTree(lastBody.get());
        assertThat(sent.get("inputs").asText()).isEqualTo(tricky);
    }

    @Test
    void acceptsASingleObjectResponse() {
        responseBody = "{\"summary_text\":\"Object form.\"}";

        assertThat(service("test-token").summarize("text")).isEqualTo("Object form.");
    }

    @Test
    void reportsTheStatusWhenTheModelFails() {
        responseStatus = 503;
        responseBody = "{\"error\":\"Model is loading\"}";

        assertThatThrownBy(() -> service("test-token").summarize("text"))
                .isInstanceOf(HuggingFaceException.class)
                .hasMessageContaining("503")
                .hasMessageContaining("Model is loading");
    }

    @Test
    void rejectsAResponseWithoutSummaryText() {
        responseBody = "{\"error\":\"something else\"}";

        assertThatThrownBy(() -> service("test-token").summarize("text"))
                .isInstanceOf(HuggingFaceException.class)
                .hasMessageContaining("Unexpected");
    }

    @Test
    void failsClearlyWhenNoTokenIsConfigured() {
        assertThatThrownBy(() -> service("").summarize("text"))
                .isInstanceOf(HuggingFaceException.class)
                .hasMessageContaining("HF_API_TOKEN");
        assertThat(lastPath.get()).isNull();
    }
}
