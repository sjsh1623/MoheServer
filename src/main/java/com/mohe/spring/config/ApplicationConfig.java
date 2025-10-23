package com.mohe.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableJpaRepositories(basePackages = "com.mohe.spring.repository")
@EnableTransactionManagement
@EnableScheduling
public class ApplicationConfig {


    @Bean
    public RestTemplate restTemplate() {
        // Configure RestTemplate with 150-second timeout for embedding operations
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(150000); // 150 seconds
        factory.setReadTimeout(150000);    // 150 seconds

        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}