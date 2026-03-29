package com.mohe.spring.batch.job;

import com.mohe.spring.batch.reader.VectorEmbeddingReader;
import com.mohe.spring.entity.EmbedStatus;
import com.mohe.spring.entity.KeywordEmbeddingLookup;
import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.KeywordEmbeddingLookupRepository;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.EmbeddingClient;
import com.mohe.spring.service.KeywordEmbeddingSaveService;
import com.mohe.spring.dto.embedding.EmbeddingResponse;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class VectorEmbeddingJobConfig {

    private final EmbeddingClient embeddingClient;
    private final PlaceRepository placeRepository;
    private final KeywordEmbeddingSaveService embeddingSaveService;
    private final KeywordEmbeddingLookupRepository lookupRepository;

    public VectorEmbeddingJobConfig(
        EmbeddingClient embeddingClient,
        PlaceRepository placeRepository,
        KeywordEmbeddingSaveService embeddingSaveService,
        KeywordEmbeddingLookupRepository lookupRepository
    ) {
        this.embeddingClient = embeddingClient;
        this.placeRepository = placeRepository;
        this.embeddingSaveService = embeddingSaveService;
        this.lookupRepository = lookupRepository;
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
                // Validate mohe_description exists
                if (place.getDescriptions().isEmpty()) {
                    System.err.println("⚠️ No description for '" + place.getName() + "' - skip");
                    return null;
                }
                String moheDescription = place.getDescriptions().get(0).getMoheDescription();
                if (moheDescription == null || moheDescription.trim().isEmpty()) {
                    System.err.println("⚠️ Empty mohe_description for '" + place.getName() + "' - skip");
                    return null;
                }

                // Validate keywords exist
                List<String> existingKeywords = place.getKeyword();
                if (existingKeywords == null || existingKeywords.isEmpty()) {
                    System.err.println("⚠️ No keywords for '" + place.getName() + "' - skip");
                    return null;
                }

                // Take first 9 keywords
                List<String> keywordsToProcess = existingKeywords.size() > 9
                    ? new ArrayList<>(existingKeywords.subList(0, 9))
                    : new ArrayList<>(existingKeywords);

                System.out.println("🧮 Vectorizing '" + place.getName() + "' — " + keywordsToProcess.size() + " keywords");

                // Delete existing embeddings for this place (if re-processing)
                embeddingSaveService.deleteEmbeddingsForPlace(place.getId());

                // === LOOKUP CACHE: check which keywords already have embeddings ===
                List<KeywordEmbeddingLookup> cached = lookupRepository.findByKeywordIn(keywordsToProcess);
                Map<String, float[]> cachedMap = new HashMap<>();
                for (KeywordEmbeddingLookup lookup : cached) {
                    cachedMap.put(lookup.getKeyword(), lookup.getEmbeddingAsArray());
                }

                // Find keywords that need new embedding
                List<String> uncachedKeywords = keywordsToProcess.stream()
                    .filter(kw -> !cachedMap.containsKey(kw))
                    .collect(Collectors.toList());

                int cacheHits = cachedMap.size();
                int cacheMisses = uncachedKeywords.size();

                // Call OpenAI only for uncached keywords
                if (!uncachedKeywords.isEmpty()) {
                    System.out.println("🚀 API call for " + cacheMisses + " new keywords (cache hit: " + cacheHits + ")");
                    EmbeddingResponse response = embeddingClient.getEmbeddings(uncachedKeywords);

                    if (!response.hasValidEmbeddings()) {
                        System.err.println("⚠️ No valid embeddings returned for '" + place.getName() + "' - skip");
                        return null;
                    }

                    List<float[]> newEmbeddings = response.getEmbeddingsAsFloatArrays();

                    // Save new embeddings to lookup cache + merge into cachedMap
                    for (int i = 0; i < Math.min(uncachedKeywords.size(), newEmbeddings.size()); i++) {
                        String kw = uncachedKeywords.get(i);
                        float[] emb = newEmbeddings.get(i);

                        // Validate non-zero
                        boolean isNonZero = false;
                        for (float v : emb) {
                            if (v != 0.0f) { isNonZero = true; break; }
                        }
                        if (!isNonZero) continue;

                        // Save to global lookup cache
                        try {
                            lookupRepository.save(new KeywordEmbeddingLookup(kw, emb));
                        } catch (Exception e) {
                            // Unique constraint violation = another thread saved it first, OK
                        }
                        cachedMap.put(kw, emb);
                    }
                } else {
                    System.out.println("✅ All " + cacheHits + " keywords from cache — no API call needed");
                }

                // Build final keyword-embedding pairs and save to place_keyword_embeddings
                List<String> finalKeywords = new ArrayList<>();
                List<float[]> finalEmbeddings = new ArrayList<>();
                for (String kw : keywordsToProcess) {
                    float[] emb = cachedMap.get(kw);
                    if (emb != null) {
                        finalKeywords.add(kw);
                        finalEmbeddings.add(emb);
                    }
                }

                if (finalEmbeddings.isEmpty()) {
                    System.err.println("⚠️ No valid embeddings for '" + place.getName() + "' after lookup");
                    return null;
                }

                int savedCount = embeddingSaveService.saveEmbeddings(
                    place.getId(), finalKeywords, finalEmbeddings
                );

                place.setEmbedStatus(EmbedStatus.COMPLETED);

                System.out.println("✅ Vectorized '" + place.getName() + "' — " +
                    savedCount + " embeddings (cache:" + cacheHits + " api:" + cacheMisses + ")");

                return place;
            } catch (Exception e) {
                System.err.println("❌ Vectorization failed for '" + place.getName() + "': " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<Place> vectorEmbeddingWriter() {
        return chunk -> {
            for (Place place : chunk.getItems()) {
                placeRepository.saveAndFlush(place);
            }
            System.out.println("💾 Saved batch of " + chunk.getItems().size() + " vectorized places");
        };
    }
}
