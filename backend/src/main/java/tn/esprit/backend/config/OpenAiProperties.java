package tn.esprit.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class OpenAiProperties {
    @Value("${openai.api.key:${OPENAI_API_KEY:}}")
    private String apiKey;

    @Value("${openai.model:${OPENAI_MODEL:gpt-4.1-mini}}")
    private String model;

    @Value("${openai.base.url:${OPENAI_BASE_URL:https://api.openai.com/v1/responses}}")
    private String baseUrl;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
