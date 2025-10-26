
package com.mohe.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private final OpenAiProperties openai = new OpenAiProperties();

    public OpenAiProperties getOpenai() {
        return openai;
    }

    public static class OpenAiProperties {
        private String apiKey = "";
        private String model = "gpt-3.5-turbo";
        private boolean active = false;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
