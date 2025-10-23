package com.mohe.spring.batch.job;

import com.mohe.spring.batch.reader.VectorEmbeddingReader;
import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.KeywordEmbeddingService;
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

@Configuration
public class VectorEmbeddingJobConfig {

    private final KeywordEmbeddingService keywordEmbeddingService;
    private final PlaceRepository placeRepository;

    public VectorEmbeddingJobConfig(
        KeywordEmbeddingService keywordEmbeddingService,
        PlaceRepository placeRepository
    ) {
        this.keywordEmbeddingService = keywordEmbeddingService;
        this.placeRepository = placeRepository;
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

                String[] keywords = existingKeywords.toArray(new String[0]);
                System.out.println("🔑 Using existing keywords for '" + place.getName() + "': " + String.join(", ", keywords));

                // Vectorize keywords using Ollama embedding
                System.out.println("🧮 Vectorizing keywords for '" + place.getName() + "'...");
                float[] keywordVector = keywordEmbeddingService.vectorizeKeywords(keywords);
                System.out.println("✅ Vector generated for '" + place.getName() + "' (dimension: " + keywordVector.length + ")");

                // Validate vector - check if it's the default empty vector
                boolean isDefaultVector = true;
                for (float v : keywordVector) {
                    if (v != 0.0f) {
                        isDefaultVector = false;
                        break;
                    }
                }

                if (isDefaultVector) {
                    System.err.println("⚠️ AI issue for '" + place.getName() + "' - Ollama vectorization failed (default empty vector)");
                    // Keep crawler_found=true, ready=false
                    placeRepository.save(place);
                    return null;
                }

                // Mark place as ready after successful vectorization
                place.setReady(true);

                // ✅ Success logging
                System.out.println("✅ Successfully vectorized '" + place.getName() + "' - " +
                    "Keywords: " + String.join(", ", place.getKeyword()) + ", " +
                    "Vector dimension: " + keywordVector.length + ", " +
                    "ready=true");

                return place;
            } catch (Exception e) {
                System.err.println("❌ Vectorization failed for '" + place.getName() + "' due to error: " + e.getMessage());
                e.printStackTrace();
                // Keep crawler_found=true, ready=false
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
