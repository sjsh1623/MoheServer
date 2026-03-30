package com.mohe.spring.batch.job;

import com.mohe.spring.batch.reader.VectorEmbeddingReader;
import com.mohe.spring.entity.EmbedStatus;
import com.mohe.spring.entity.KeywordEmbeddingLookup;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceDescriptionEmbedding;
import com.mohe.spring.repository.KeywordEmbeddingLookupRepository;
import com.mohe.spring.repository.PlaceDescriptionEmbeddingRepository;
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
    private final PlaceDescriptionEmbeddingRepository descEmbeddingRepository;

    public VectorEmbeddingJobConfig(
        EmbeddingClient embeddingClient,
        PlaceRepository placeRepository,
        KeywordEmbeddingSaveService embeddingSaveService,
        KeywordEmbeddingLookupRepository lookupRepository,
        PlaceDescriptionEmbeddingRepository descEmbeddingRepository
    ) {
        this.embeddingClient = embeddingClient;
        this.placeRepository = placeRepository;
        this.embeddingSaveService = embeddingSaveService;
        this.lookupRepository = lookupRepository;
        this.descEmbeddingRepository = descEmbeddingRepository;
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

                // === 문장 임베딩 필요 여부 체크 ===
                boolean needDescEmbedding = !descEmbeddingRepository.existsByPlaceId(place.getId());
                float[] descriptionEmbedding = null;

                // API 호출: 캐시 미스 키워드 + 문장(필요시)을 한번에
                if (!uncachedKeywords.isEmpty() || needDescEmbedding) {
                    List<String> textsToEmbed = new ArrayList<>(uncachedKeywords);
                    if (needDescEmbedding) {
                        textsToEmbed.add(moheDescription); // 문장을 마지막에 추가
                    }

                    System.out.println("🚀 API call: " + uncachedKeywords.size() + " keywords + " +
                        (needDescEmbedding ? "1 description" : "0 description") +
                        " (cache hit: " + cacheHits + ")");

                    EmbeddingResponse response = embeddingClient.getEmbeddings(textsToEmbed);

                    if (!response.hasValidEmbeddings()) {
                        System.err.println("⚠️ No valid embeddings returned for '" + place.getName() + "' - skip");
                        return null;
                    }

                    List<float[]> allEmbeddings = response.getEmbeddingsAsFloatArrays();

                    // 문장 임베딩 분리 (마지막 요소)
                    if (needDescEmbedding && allEmbeddings.size() == textsToEmbed.size()) {
                        descriptionEmbedding = allEmbeddings.get(allEmbeddings.size() - 1);
                    }

                    // 키워드 임베딩 처리 (문장 제외)
                    int keywordEmbCount = Math.min(uncachedKeywords.size(), allEmbeddings.size());
                    for (int i = 0; i < keywordEmbCount; i++) {
                        String kw = uncachedKeywords.get(i);
                        float[] emb = allEmbeddings.get(i);

                        boolean isNonZero = false;
                        for (float v : emb) {
                            if (v != 0.0f) { isNonZero = true; break; }
                        }
                        if (!isNonZero) continue;

                        if (lookupRepository.findByKeyword(kw).isEmpty()) {
                            try {
                                lookupRepository.saveAndFlush(new KeywordEmbeddingLookup(kw, emb));
                            } catch (Exception e) {
                                try { lookupRepository.flush(); } catch (Exception ignored) {}
                            }
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

                // === 문장 임베딩 저장 ===
                if (descriptionEmbedding != null) {
                    try {
                        descEmbeddingRepository.deleteByPlaceId(place.getId());
                        descEmbeddingRepository.saveAndFlush(
                            new PlaceDescriptionEmbedding(place.getId(), moheDescription, descriptionEmbedding)
                        );
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to save description embedding for '" + place.getName() + "': " + e.getMessage());
                    }
                } else if (!needDescEmbedding) {
                    // 이미 문장 임베딩 있음 — skip
                }

                place.setEmbedStatus(EmbedStatus.COMPLETED);

                System.out.println("✅ Vectorized '" + place.getName() + "' — " +
                    savedCount + " kw + " + (descriptionEmbedding != null ? "1 desc" : "desc cached") +
                    " (cache:" + cacheHits + " api:" + cacheMisses + ")");

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
