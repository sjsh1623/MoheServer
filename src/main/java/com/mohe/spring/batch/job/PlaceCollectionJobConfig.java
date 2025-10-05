package com.mohe.spring.batch.job;

import com.mohe.spring.entity.Place;
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

/**
 * 장소 데이터 수집 Batch Job 설정
 *
 * 흐름:
 * 1. Reader: 지역/카테고리 조합으로 검색 쿼리 생성
 * 2. Processor: Naver API 호출 → Place 객체 변환 → Google API로 보강 → 필터링
 * 3. Writer: DB에 Place 저장
 */
@Configuration
public class PlaceCollectionJobConfig {

    /**
     * Place 수집 Job
     */
    @Bean
    public Job placeCollectionJob(
            JobRepository jobRepository,
            Step placeCollectionStep) {
        return new JobBuilder("placeCollectionJob", jobRepository)
                .start(placeCollectionStep)
                .build();
    }

    /**
     * Place 수집 Step
     */
    @Bean
    public Step placeCollectionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<String> placeQueryReader,
            ItemProcessor<String, Place> placeProcessor,
            ItemWriter<Place> placeWriter) {
        return new StepBuilder("placeCollectionStep", jobRepository)
                .<String, Place>chunk(10, transactionManager) // 10개씩 처리
                .reader(placeQueryReader)
                .processor(placeProcessor)
                .writer(placeWriter)
                .build();
    }
}
