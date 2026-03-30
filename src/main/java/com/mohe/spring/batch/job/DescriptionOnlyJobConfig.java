package com.mohe.spring.batch.job;

import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceDescription;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.OpenAiDescriptionService;
import com.mohe.spring.service.OpenAiDescriptionService.DescriptionPayload;
import com.mohe.spring.service.OpenAiDescriptionService.DescriptionResult;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 크롤링 없이 OpenAI만 호출하여 mohe_description + 키워드 생성
 * 대상: crawl_status=COMPLETED이지만 mohe_description이 없는 장소
 * 기존 리뷰/카테고리/주소 데이터를 활용
 */
@Slf4j
@Configuration
public class DescriptionOnlyJobConfig {

    private final PlaceRepository placeRepository;
    private final OpenAiDescriptionService openAiDescriptionService;
    private final EntityManager entityManager;

    public DescriptionOnlyJobConfig(
            PlaceRepository placeRepository,
            OpenAiDescriptionService openAiDescriptionService,
            EntityManager entityManager) {
        this.placeRepository = placeRepository;
        this.openAiDescriptionService = openAiDescriptionService;
        this.entityManager = entityManager;
    }

    @Bean
    public Job descriptionOnlyJob(JobRepository jobRepository, Step descriptionOnlyStep) {
        return new JobBuilder("descriptionOnlyJob", jobRepository)
                .start(descriptionOnlyStep)
                .build();
    }

    @Bean
    public Step descriptionOnlyStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("descriptionOnlyStep", jobRepository)
                .<Place, Place>chunk(5, transactionManager)
                .reader(descriptionOnlyReader())
                .processor(descriptionOnlyProcessor())
                .writer(descriptionOnlyWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    public ItemReader<Place> descriptionOnlyReader() {
        return new ItemReader<>() {
            private Iterator<Long> idIterator;

            @Override
            public Place read() {
                if (idIterator == null) {
                    // COMPLETED인데 mohe_description이 없는 장소 ID 조회
                    @SuppressWarnings("unchecked")
                    List<Long> ids = entityManager.createNativeQuery("""
                        SELECT p.id FROM places p
                        WHERE p.crawl_status = 'COMPLETED'
                        AND NOT EXISTS (
                            SELECT 1 FROM place_descriptions pd
                            WHERE pd.place_id = p.id
                            AND pd.mohe_description IS NOT NULL
                            AND pd.mohe_description <> ''
                        )
                        ORDER BY p.id ASC
                        LIMIT 5000
                    """).getResultList();

                    log.info("📋 DescriptionOnlyJob: {} places need AI description", ids.size());
                    idIterator = ids.stream().map(Number.class::cast).map(Number::longValue).iterator();
                }

                if (!idIterator.hasNext()) return null;

                Long id = idIterator.next();
                return placeRepository.findById(id).orElse(null);
            }
        };
    }

    @Bean
    public ItemProcessor<Place, Place> descriptionOnlyProcessor() {
        return place -> {
            try {
                // 기존 데이터 수집
                String reviews = "";
                if (place.getReviews() != null && !place.getReviews().isEmpty()) {
                    reviews = place.getReviews().stream()
                            .limit(10)
                            .map(r -> r.getReviewText())
                            .filter(t -> t != null && !t.isBlank())
                            .collect(Collectors.joining("\n"));
                }

                String category = "";
                if (place.getCategory() != null && !place.getCategory().isEmpty()) {
                    category = String.join(", ", place.getCategory());
                }

                String origDesc = "";
                if (place.getDescriptions() != null && !place.getDescriptions().isEmpty()) {
                    String od = place.getDescriptions().get(0).getOriginalDescription();
                    if (od != null && !od.isBlank()) origDesc = od;
                }

                boolean petFriendly = place.getPetFriendly() != null && place.getPetFriendly();

                // OpenAI 호출
                DescriptionPayload payload = new DescriptionPayload(
                        "", reviews, origDesc, category, petFriendly
                );

                Optional<DescriptionResult> resultOpt = openAiDescriptionService.generateDescription(payload);

                if (resultOpt.isEmpty()) {
                    log.warn("⚠️ OpenAI returned empty for '{}'", place.getName());
                    return null;
                }

                DescriptionResult result = resultOpt.get();
                String moheDesc = result.description();
                List<String> keywords = result.keywords();

                if (moheDesc == null || moheDesc.isBlank()) {
                    // Fallback: 장소명 + 카테고리로 간단 설명
                    moheDesc = place.getName() + "에 대한 정보입니다.";
                }

                // 키워드 보정 (9개 미만이면 패딩)
                if (keywords == null) keywords = new ArrayList<>();
                while (keywords.size() < 9) {
                    keywords.add("장소");
                }

                // 설명 저장
                if (place.getDescriptions() == null || place.getDescriptions().isEmpty()) {
                    PlaceDescription desc = new PlaceDescription();
                    desc.setPlace(place);
                    desc.setMoheDescription(moheDesc);
                    place.getDescriptions().add(desc);
                } else {
                    place.getDescriptions().get(0).setMoheDescription(moheDesc);
                }

                // 키워드 저장
                place.setKeyword(keywords);

                // embed_status를 PENDING으로 → VectorEmbeddingJob이 처리
                place.setEmbedStatus(com.mohe.spring.entity.EmbedStatus.PENDING);

                log.info("✅ AI desc generated for '{}': {}... | keywords: {}",
                        place.getName(),
                        moheDesc.substring(0, Math.min(40, moheDesc.length())),
                        keywords.size());

                return place;
            } catch (Exception e) {
                log.error("❌ Failed to generate desc for '{}': {}", place.getName(), e.getMessage());
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<Place> descriptionOnlyWriter() {
        return chunk -> {
            for (Place place : chunk.getItems()) {
                try {
                    placeRepository.saveAndFlush(place);
                } catch (Exception e) {
                    log.error("❌ Failed to save '{}': {}", place.getName(), e.getMessage());
                }
            }
            log.info("💾 Saved {} places with new AI descriptions", chunk.getItems().size());
        };
    }
}
