package com.mohe.spring.dto.embedding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the result of batch embedding processing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchEmbeddingResult {

    /**
     * Total number of places processed
     */
    private int totalPlaces;

    /**
     * Number of places successfully processed
     */
    private int successfulPlaces;

    /**
     * Number of places that failed processing
     */
    private int failedPlaces;

    /**
     * Number of places skipped (no keywords)
     */
    private int skippedPlaces;

    /**
     * Total number of embeddings created
     */
    private int totalEmbeddings;

    /**
     * Processing time in milliseconds
     */
    private long processingTimeMs;

    /**
     * Error message if batch processing failed
     */
    private String errorMessage;

    /**
     * Get success rate as percentage
     */
    public double getSuccessRate() {
        if (totalPlaces == 0) {
            return 0.0;
        }
        return (double) successfulPlaces / totalPlaces * 100.0;
    }

    /**
     * Get processing time in seconds
     */
    public double getProcessingTimeSec() {
        return processingTimeMs / 1000.0;
    }
}
