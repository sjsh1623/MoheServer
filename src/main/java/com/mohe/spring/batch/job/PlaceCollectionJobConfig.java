package com.mohe.spring.batch.job;

import com.mohe.spring.batch.location.LocationRegistry;
import com.mohe.spring.batch.reader.PlaceQueryReader;
import com.mohe.spring.entity.Place;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Place Collection Batch Job 설정
 *
 * <p>Naver API를 통해 장소 데이터를 수집하여 데이터베이스에 저장하는 배치 Job을 정의합니다.</p>
 *
 * <h3>Job 구성</h3>
 * <ul>
 *   <li><b>Reader:</b> PlaceQueryReader - 지역 + 카테고리 조합 쿼리 생성</li>
 *   <li><b>Processor:</b> PlaceDataProcessor - Naver API 호출 및 필터링</li>
 *   <li><b>Writer:</b> PlaceDataWriter - DB 저장</li>
 * </ul>
 *
 * <h3>Region 기반 처리</h3>
 * <p>Job 파라미터로 "region"을 전달하여 특정 지역만 처리할 수 있습니다:</p>
 * <ul>
 *   <li><b>"seoul":</b> 서울특별시만 처리</li>
 *   <li><b>"jeju":</b> 제주특별자치도만 처리</li>
 *   <li><b>"yongin":</b> 경기도 용인특례시만 처리</li>
 *   <li><b>null 또는 기타:</b> 모든 지역 처리</li>
 * </ul>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see com.mohe.spring.batch.reader.PlaceQueryReader
 * @see com.mohe.spring.batch.processor.PlaceDataProcessor
 * @see com.mohe.spring.batch.writer.PlaceDataWriter
 */
@Configuration
public class PlaceCollectionJobConfig {

    /**
     * 장소 수집 메인 Job 정의
     *
     * <p>하나의 Step으로 구성된 간단한 Job입니다.
     * 필요 시 추가 Step을 체이닝할 수 있습니다.</p>
     *
     * @param jobRepository Spring Batch 메타데이터 저장소
     * @param placeCollectionStep 장소 수집 Step (Reader → Processor → Writer)
     * @return 실행 가능한 Job 인스턴스
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
     * 장소 수집 Step 정의
     *
     * <p>Chunk-oriented processing 방식으로 동작합니다:
     * <ul>
     *   <li>Chunk Size: 10 (한 번에 10개 아이템씩 처리)</li>
     *   <li>Input Type: String (검색 쿼리)</li>
     *   <li>Output Type: Place (장소 엔티티)</li>
     * </ul>
     * </p>
     *
     * <p><b>트랜잭션 관리</b>: Chunk 단위로 트랜잭션이 커밋됩니다.
     * 즉, 10개 처리 후 커밋되며, 중간에 실패 시 해당 Chunk만 롤백됩니다.</p>
     *
     * @param jobRepository Spring Batch 메타데이터 저장소
     * @param transactionManager 트랜잭션 관리자
     * @param placeQueryReader 검색 쿼리를 읽어오는 Reader (region 파라미터 적용)
     * @param placeProcessor 검색 쿼리를 Place로 변환하는 Processor
     * @param placeWriter Place를 DB에 저장하는 Writer
     * @return 실행 가능한 Step 인스턴스
     */
    @Bean
    public Step placeCollectionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<String> placeQueryReader,
            @Qualifier("placeDataProcessor") ItemProcessor<String, Place> placeProcessor,
            ItemWriter<Place> placeWriter) {
        return new StepBuilder("placeCollectionStep", jobRepository)
                .<String, Place>chunk(10, transactionManager) // 10개씩 chunk 처리
                .reader(placeQueryReader)      // 검색 쿼리 생성
                .processor(placeProcessor)     // API 호출 및 변환
                .writer(placeWriter)           // DB 저장
                .build();
    }

    /**
     * Region 기반 PlaceQueryReader 생성 (Step Scope)
     *
     * <p>Job 파라미터로 전달된 region 값에 따라 PlaceQueryReader를 동적으로 생성합니다.
     * @StepScope를 사용하여 각 Job 실행마다 새로운 인스턴스가 생성됩니다.</p>
     *
     * <h3>Region 파라미터</h3>
     * <p>Job 실행 시 전달되는 파라미터:</p>
     * <pre>
     * JobParameters params = new JobParametersBuilder()
     *     .addString("region", "seoul")  // 서울만 처리
     *     .toJobParameters();
     * </pre>
     *
     * @param locationRegistry 지역 정보 레지스트리
     * @param region Job 파라미터로 전달된 지역 코드 (nullable)
     * @return 설정된 PlaceQueryReader 인스턴스
     */
    @Bean
    @StepScope
    public PlaceQueryReader placeQueryReader(
            LocationRegistry locationRegistry,
            @Value("#{jobParameters['region']}") String region) {
        PlaceQueryReader reader = new PlaceQueryReader(locationRegistry);
        reader.setRegion(region);
        return reader;
    }
}
