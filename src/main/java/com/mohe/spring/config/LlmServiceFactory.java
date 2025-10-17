
package com.mohe.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.service.LlmService;
import com.mohe.spring.service.KeywordEmbeddingService;
import com.mohe.spring.service.OpenAiService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class LlmServiceFactory {

    @Bean
    @Profile("openai")
    public LlmService openAiService(LlmProperties llmProperties, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        return new OpenAiService(llmProperties, restTemplateBuilder, objectMapper);
    }

    @Bean(name = "llmService")
    @Profile("ollama")
    public LlmService ollamaLlmService(LlmProperties llmProperties, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        return new KeywordEmbeddingService(llmProperties, webClientBuilder, objectMapper);
    }
}
