package com.mohe.spring.batch.job;

import com.mohe.spring.batch.reader.VectorEmbeddingReader;
import com.mohe.spring.entity.EmbedStatus;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceKeywordEmbedding;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.repository.PlaceKeywordEmbeddingRepository;
import com.mohe.spring.service.EmbeddingClient;
import com.mohe.spring.service.KeywordEmbeddingSaveService;
import com.mohe.spring.dto.embedding.EmbeddingResponse;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.ArrayList;

@Configuration
public class VectorEmbeddingJobConfig {

    private final EmbeddingClient embeddingClient;
    private final PlaceRepository placeRepository;
    private final KeywordEmbeddingSaveService embeddingSaveService;

    public VectorEmbeddingJobConfig(
        EmbeddingClient embeddingClient,
        PlaceRepository placeRepository,
        KeywordEmbeddingSaveService embeddingSaveService
    ) {
        this.embeddingClient = embeddingClient;
        this.placeRepository = placeRepository;
        this.embeddingSaveService = embeddingSaveService;
    }

    @Bean
    public Job vectorEmbeddingJob(JobRepository jobRepository, Step vectorEmbeddingStep) {
        return new JobBuilder("vectorEmbeddingJob", jobRepository)
                .start(vectorEmbeddingStep)
                .build();
    }

    @Bean
    public Step vectorEmbeddingStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        VectorEmbeddingReader vectorEmbeddingReader,
        ItemProcessor<Place, Place> vectorEmbeddingProcessor,
        ItemWriter<Place> vectorEmbeddingWriter
    ) {
        return new StepBuilder("vectorEmbeddingStep", jobRepository)
                .<Place, Place>chunk(5, transactionManager)
                .reader(vectorEmbeddingReader)
                .processor(vectorEmbeddingProcessor)
                .writer(vectorEmbeddingWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    public ItemProcessor<Place, Place> vectorEmbeddingProcessor() {
        return place -> {
            try {
                // Check if place has mohe_description
                if (place.getDescriptions().isEmpty()) {
                    System.err.println("⚠️ No description found for '" + place.getName() + "' - skipping vectorization");
                    return null;
                }

                String moheDescription = place.getDescriptions().get(0).getMoheDescription();
                if (moheDescription == null || moheDescription.trim().isEmpty()) {
                    System.err.println("⚠️ Empty mohe_description for '" + place.getName() + "' - skipping vectorization");
                    return null;
                }

                System.out.println("🧮 Starting vectorization for '" + place.getName() + "'...");

                // Get keywords that were already extracted during crawling
                List<String> existingKeywords = place.getKeyword();
                if (existingKeywords == null || existingKeywords.isEmpty()) {
                    System.err.println("⚠️ No keywords found for '" + place.getName() + "' - skipping vectorization");
                    return null;
                }

                // Take only first 9 keywords
                List<String> keywordsToProcess = existingKeywords.size() > 9
                    ? existingKeywords.subList(0, 9)
                    : existingKeywords;

                System.out.println("🔑 Processing " + keywordsToProcess.size() + " keywords for '" + place.getName() + "': " + String.join(", ", keywordsToProcess));

                // Delete existing embeddings for this place (if re-processing)
                embeddingSaveService.deleteEmbeddingsForPlace(place.getId());

                // Call embedding service to get individual embeddings for each keyword
                System.out.println("🚀 Calling embedding service for place_id=" + place.getId() + " with " + keywordsToProcess.size() + " keywords");
                EmbeddingResponse response = embeddingClient.getEmbeddings(keywordsToProcess);

                // Validate response
                if (!response.hasValidEmbeddings()) {
                    System.err.println("⚠️ No valid embeddings returned for '" + place.getName() + "' - skipping");
                    return null;
                }

                List<float[]> embeddings = response.getEmbeddingsAsFloatArrays();
                System.out.println("✅ Received " + embeddings.size() + " embeddings for '" + place.getName() + "'");

                if (embeddings.size() != keywordsToProcess.size()) {
                    System.err.println("⚠️ Embedding count mismatch for '" + place.getName() + "': expected " + keywordsToProcess.size() + ", got " + embeddings.size());
                    // Continue with available embeddings
                }

                // Validate embeddings - check if they're non-empty
                int validEmbeddings = 0;
                for (float[] embedding : embeddings) {
                    boolean isNonZero = false;
                    for (float v : embedding) {
                        if (v != 0.0f) {
                            isNonZero = true;
                            break;
                        }
                    }
                    if (isNonZero) validEmbeddings++;
                }

                if (validEmbeddings == 0) {
                    System.err.println("⚠️ All embeddings are zero vectors for '" + place.getName() + "' - embedding service may have failed");
                    return null;
                }

                // Save embeddings using service with new transaction
                int savedCount = embeddingSaveService.saveEmbeddings(
                    place.getId(),
                    keywordsToProcess,
                    embeddings
                );

                System.out.println("💾 Saved " + savedCount + " embeddings for place_id=" + place.getId());

                // Mark place as embed_status=COMPLETED after successful vectorization
                place.setEmbedStatus(EmbedStatus.COMPLETED);

                // ✅ Success logging
                System.out.println("✅ Successfully vectorized '" + place.getName() + "' - " +
                    "Keywords: " + String.join(", ", keywordsToProcess) + ", " +
                    "Vector dimension: 1792, " +
                    "Saved " + savedCount + " embeddings, " +
                    "embed_status=COMPLETED");

                return place;
            } catch (Exception e) {
                System.err.println("❌ Vectorization failed for '" + place.getName() + "' due to error: " + e.getMessage());
                e.printStackTrace();
                // Keep crawl_status=COMPLETED, embed_status=PENDING
                placeRepository.save(place);
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<Place> vectorEmbeddingWriter() {
        return chunk -> {
            // Save places individually and flush after each to avoid Hibernate session conflicts
            for (Place place : chunk.getItems()) {
                placeRepository.saveAndFlush(place);
            }
            // Log batch write success
            System.out.println("💾 Saved batch of " + chunk.getItems().size() + " vectorized places to database");
        };
    }
}
