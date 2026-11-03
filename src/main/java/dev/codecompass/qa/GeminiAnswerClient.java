package dev.codecompass.qa;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiAnswerClient {
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiAnswerClient(RestClient.Builder restClientBuilder,
                              @Value("${codecompass.gemini.api-key:}") String apiKey,
                              @Value("${codecompass.gemini.model}") String model) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Gemini is selected, but no API key is configured. Add gemini_api_key to .secrets or set GEMINI_API_KEY.");
        }

        Map<String, Object> request = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("temperature", 0.1, "maxOutputTokens", 1200));

        JsonNode response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        JsonNode text = response == null ? null : response.at("/candidates/0/content/parts/0/text");
        if (text == null || text.isMissingNode() || text.asText().isBlank()) {
            throw new IllegalStateException("Gemini returned no answer text.");
        }
        return text.asText();
    }
}
