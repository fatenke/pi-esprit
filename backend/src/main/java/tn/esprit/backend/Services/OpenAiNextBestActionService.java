package tn.esprit.backend.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tn.esprit.backend.DTO.NextBestActionAiResult;
import tn.esprit.backend.config.OpenAiProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiNextBestActionService implements NextBestActionAIService {
    private static final String SYSTEM_PROMPT = """
            Tu es un assistant IA specialise dans les deals d'investissement startup-investisseur.
            Ton role est de recommander la meilleure prochaine action operationnelle.
            Reponds uniquement en JSON valide.
            Ne donne pas de conseil juridique ou financier definitif.
            Base ta recommandation uniquement sur les donnees fournies.
            """;

    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Override
    public NextBestActionAiResult generateAction(NextBestActionPromptContext context) {
        if (!openAiProperties.isConfigured()) {
            throw new IllegalStateException("OpenAI API key is not configured on the backend.");
        }

        try {
            String payload = objectMapper.writeValueAsString(buildRequestBody(context));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openAiProperties.getBaseUrl()))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            log.info("Calling OpenAI for next best action generation on requestId={}", context.getInvestmentRequestId());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value() || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                log.warn("OpenAI call failed for requestId={} with status={} body={}",
                        context.getInvestmentRequestId(),
                        response.statusCode(),
                        truncate(response.body()));
                throw new IllegalStateException("OpenAI API returned status " + response.statusCode() + ".");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String refusal = extractRefusal(root);
            if (refusal != null && !refusal.isBlank()) {
                log.warn("OpenAI refused next best action generation for requestId={}: {}", context.getInvestmentRequestId(), refusal);
                throw new IllegalStateException("OpenAI refused to generate a recommendation.");
            }

            String outputText = extractOutputText(root);
            if (outputText == null || outputText.isBlank()) {
                throw new IllegalStateException("OpenAI returned an empty recommendation payload.");
            }

            NextBestActionAiResult parsed = objectMapper.readValue(outputText, NextBestActionAiResult.class);
            log.info("OpenAI recommendation generated for requestId={} actorRole={} priority={}",
                    context.getInvestmentRequestId(), parsed.getActorRole(), parsed.getPriority());
            return parsed;
        } catch (IOException e) {
            log.error("Failed to parse OpenAI response for requestId={}", context.getInvestmentRequestId(), e);
            throw new IllegalStateException("Failed to parse OpenAI JSON response.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("OpenAI request interrupted for requestId={}", context.getInvestmentRequestId(), e);
            throw new IllegalStateException("OpenAI request was interrupted.", e);
        }
    }

    private Map<String, Object> buildRequestBody(NextBestActionPromptContext context) throws IOException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", openAiProperties.getModel());
        requestBody.put("store", false);
        requestBody.put("input", List.of(
                Map.of(
                        "role", "system",
                        "content", List.of(Map.of("type", "input_text", "text", SYSTEM_PROMPT))
                ),
                Map.of(
                        "role", "user",
                        "content", List.of(Map.of("type", "input_text", "text", buildUserPrompt(context)))
                )
        ));
        requestBody.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", "next_best_action",
                        "strict", true,
                        "schema", buildSchema()
                )
        ));
        return requestBody;
    }

    private Map<String, Object> buildSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "actorRole", Map.of("type", "string", "enum", List.of("INVESTOR", "STARTUP", "ADMIN")),
                        "title", Map.of("type", "string"),
                        "description", Map.of("type", "string"),
                        "priority", Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH", "URGENT")),
                        "reason", Map.of("type", "string"),
                        "dueInDays", Map.of("type", "integer")
                ),
                "required", List.of("actorRole", "title", "description", "priority", "reason", "dueInDays")
        );
    }

    private String buildUserPrompt(NextBestActionPromptContext context) throws IOException {
        return """
                Analyse le deal suivant et recommande une seule prochaine action operationnelle concrete.
                Retourne uniquement l'objet JSON attendu.

                Contexte du deal:
                %s
                """.formatted(
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context)
        );
    }

    private String extractOutputText(JsonNode root) {
        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual()) {
            return outputText.asText();
        }

        for (JsonNode outputItem : root.path("output")) {
            for (JsonNode content : outputItem.path("content")) {
                if ("output_text".equals(content.path("type").asText()) && content.path("text").isTextual()) {
                    return content.path("text").asText();
                }
            }
        }
        return null;
    }

    private String extractRefusal(JsonNode root) {
        JsonNode rootRefusal = root.path("refusal");
        if (rootRefusal.isTextual()) {
            return rootRefusal.asText();
        }
        for (JsonNode outputItem : root.path("output")) {
            for (JsonNode content : outputItem.path("content")) {
                if ("refusal".equals(content.path("type").asText()) && content.path("refusal").isTextual()) {
                    return content.path("refusal").asText();
                }
            }
        }
        return null;
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        if (body.length() <= 500) {
            return body;
        }
        return body.substring(0, 500) + "...";
    }
}
