package com.soulmate.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.soulmate.backend.config.BackendProperties;
import com.soulmate.backend.dto.ai.PredictMoodResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final BackendProperties.Gemini geminiProperties;
    private final RestClient restClient;

    public GeminiService(BackendProperties backendProperties, RestClient.Builder restClientBuilder) {
        this.geminiProperties = backendProperties.getGemini();
        this.restClient = restClientBuilder.build();
    }

    public PredictMoodResponse predictMood(String text) {
        String normalizedText = text == null ? "" : text.trim();
        if (!StringUtils.hasText(geminiProperties.getApiKey())) {
            return buildLocalFallback(normalizedText, "missing_api_key");
        }

        String prompt = buildPrompt(normalizedText);
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
            + geminiProperties.getModel()
            + ":generateContent?key=" + geminiProperties.getApiKey();

        Map<String, Object> payload = Map.of(
            "contents", List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))
            ),
            "generationConfig", Map.of(
                "temperature", 0.2,
                "maxOutputTokens", 20
            )
        );

        try {
            JsonNode response = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

            String raw = extractGeminiText(response);
            String mood = normalizeMood(raw);
            return new PredictMoodResponse(mood, raw, geminiProperties.getModel());
        } catch (RestClientException e) {
            return buildLocalFallback(normalizedText, "gemini_call_failed");
        }
    }

    private String buildPrompt(String text) {
        return """
            You are a mental health assistant.
            Analyze the diary text and return exactly one English mood word.
            Examples: Happy, Sad, Angry, Neutral, Excited, Tired.
            If unclear, return Neutral.
            Diary: \"%s\"
            """.formatted(text);
    }

    private String extractGeminiText(JsonNode root) {
        if (root == null) {
            return "Neutral";
        }
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return "Neutral";
        }
        JsonNode first = candidates.get(0);
        JsonNode parts = first.path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return "Neutral";
        }
        for (JsonNode part : parts) {
            String text = part.path("text").asText(null);
            if (StringUtils.hasText(text)) {
                return text.trim();
            }
        }
        return "Neutral";
    }

    private String normalizeMood(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "Neutral";
        }
        String[] parts = raw.trim().split("[\\s,.:;!?]+");
        return parts.length == 0 ? "Neutral" : parts[0];
    }

    private PredictMoodResponse buildLocalFallback(String text, String reason) {
        String mood = classifyMoodHeuristic(text);
        return new PredictMoodResponse(mood, "fallback:" + reason, "local-heuristic");
    }

    private String classifyMoodHeuristic(String text) {
        String t = text == null ? "" : text.toLowerCase();

        if (containsAny(t, "angry", "mad", "furious", "annoyed", "frustrated", "upset")) {
            return "Angry";
        }
        if (containsAny(t, "sad", "unhappy", "depressed", "lonely", "cry", "hopeless")) {
            return "Sad";
        }
        if (containsAny(t, "tired", "sleepy", "exhausted", "drained", "burnout")) {
            return "Tired";
        }
        if (containsAny(t, "happy", "excited", "great", "good", "joy", "productive", "amazing")) {
            return "Happy";
        }
        return "Neutral";
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
