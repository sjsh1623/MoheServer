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
 * <p>Spring Batch를 사용하여 외부 API(Naver, Google)로부터 장소 데이터를 수집하고
 * 데이터베이스에 저장하는 배치 작업을 정의합니다.</p>
 *
 * <h3>배치 처리 흐름</h3>
 * <ol>
 *   <li><b>Reader</b>: 지역명 + 카테고리 조합으로 검색 쿼리 생성 (예: "강남구 카페")</li>
 *   <li><b>Processor</b>:
 *     <ul>
 *       <li>Naver Local Search API 호출하여 장소 검색</li>
 *       <li>Place 엔티티로 변환</li>
 *       <li>Google Places API로 평점 및 상세 정보 보강</li>
 *       <li>편의점/마트 등 불필요한 장소 필터링</li>
 *       <li>중복 체크</li>
 *     </ul>
 *   </li>
 *   <li><b>Writer</b>: 검증된 Place 엔티티를 DB에 저장 (10개씩 chunk 처리)</li>
 * </ol>
 *
 * <h3>실행 방법</h3>
 * <pre>
 * POST /api/batch/jobs/place-collection
 * </pre>
 *
 * @see com.mohe.spring.batch.reader.PlaceQueryReader
 * @see com.mohe.spring.batch.processor.PlaceDataProcessor
 * @see com.mohe.spring.batch.writer.PlaceDataWriter
 * @author Andrew Lim
 * @since 1.0
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
     * @param placeQueryReader 검색 쿼리를 읽어오는 Reader
     * @param placeProcessor 검색 쿼리를 Place로 변환하는 Processor
     * @param placeWriter Place를 DB에 저장하는 Writer
     * @return 실행 가능한 Step 인스턴스
     */
    @Bean
    public Step placeCollectionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<String> placeQueryReader,
            ItemProcessor<String, Place> placeProcessor,
            ItemWriter<Place> placeWriter) {
        return new StepBuilder("placeCollectionStep", jobRepository)
                .<String, Place>chunk(10, transactionManager) // 10개씩 chunk 처리
                .reader(placeQueryReader)      // 검색 쿼리 생성
                .processor(placeProcessor)     // API 호출 및 변환
                .writer(placeWriter)           // DB 저장
                .build();
    }
}
