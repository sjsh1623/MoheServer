
package com.mohe.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private final OpenAiProperties openai = new OpenAiProperties();
    private final OllamaProperties ollama = new OllamaProperties();

    public OpenAiProperties getOpenai() {
        return openai;
    }

    public OllamaProperties getOllama() {
        return ollama;
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

    public static class OllamaProperties {
        private String baseUrl = "http://localhost:11434";
        private String model = "kanana-instruct";
        private int timeout = 120;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
}
