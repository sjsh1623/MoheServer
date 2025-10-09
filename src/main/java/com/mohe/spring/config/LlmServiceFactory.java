
package com.mohe.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.service.LlmService;
import com.mohe.spring.service.OllamaService;
import com.mohe.spring.service.OpenAiService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class LlmServiceFactory {

    @Bean
    public LlmService llmService(LlmProperties llmProperties, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        if (llmProperties.isOpenAiActive()) {
            return new OpenAiService(llmProperties);
        } else {
            return new OllamaService(llmProperties, webClientBuilder, objectMapper);
        }
    }
}
