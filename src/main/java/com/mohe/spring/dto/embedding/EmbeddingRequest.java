package com.mohe.spring.dto.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for Kanana embedding service
 * POST /embed endpoint expects an array of strings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {

    /**
     * List of text strings to be embedded
     * Example: ["아늑한 카페", "분위기 좋은 음악"]
     */
    @JsonProperty("texts")
    private List<String> texts;

    /**
     * Convenience constructor for single text embedding
     */
    public EmbeddingRequest(String text) {
        this.texts = List.of(text);
    }
}
