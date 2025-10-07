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
 * 웹 크롤링 배치 Job 설정
 *
 * <p>Python 크롤링 서버와 연동하여 웹에서 장소 데이터를 수집합니다.
 * PlaceCollectionJob과 독립적으로 실행되며, 다른 데이터 소스를 대상으로 합니다.</p>
 *
 * <h3>주요 특징</h3>
 * <ul>
 *   <li><b>데이터 소스</b>: Python 크롤링 서버 (별도 프로세스)</li>
 *   <li><b>처리 방식</b>: Chunk-oriented processing (10개씩)</li>
 *   <li><b>저장 대상</b>: places 테이블 (PlaceCollectionJob과 동일)</li>
 *   <li><b>실행 방식</b>: REST API 수동 트리거</li>
 * </ul>
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li><b>Reader</b>: Python 크롤러에게 작업 요청 (크롤링할 URL 또는 키워드 전달)</li>
 *   <li><b>Processor</b>: 크롤링된 데이터를 Place 엔티티로 변환 및 검증</li>
 *   <li><b>Writer</b>: PlaceDataWriter 재사용하여 DB 저장</li>
 * </ol>
 *
 * <h3>Python 서버 연동 방식</h3>
 * <p>CrawlerTaskReader가 Python 크롤링 서버에 HTTP 요청을 보내고,
 * 응답받은 크롤링 결과를 Spring Batch 파이프라인으로 전달합니다.</p>
 *
 * <pre>
 * Python Server (FastAPI/Flask)
 *   ↓ HTTP Request (POST /crawl)
 *   ↓ JSON Response (크롤링 데이터)
 * CrawlerTaskReader → CrawledDataProcessor → PlaceDataWriter
 * </pre>
 *
 * <h3>트랜잭션 관리</h3>
 * <p>Chunk 단위(10개)로 트랜잭션이 커밋되며,
 * 크롤링 실패 시 해당 Chunk만 롤백되어 다른 데이터는 영향받지 않습니다.</p>
 *
 * <h3>실행 방법</h3>
 * <pre>
 * POST /api/batch/jobs/web-crawling
 * </pre>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see com.mohe.spring.batch.reader.CrawlerTaskReader
 * @see com.mohe.spring.batch.processor.CrawledDataProcessor
 * @see com.mohe.spring.batch.writer.PlaceDataWriter
 */
@Configuration
public class WebCrawlingJobConfig {

    /**
     * 웹 크롤링 메인 Job 정의
     *
     * <p>Python 크롤러 연동 배치 작업입니다.
     * PlaceCollectionJob과 독립적으로 실행되며, 다른 Step을 체이닝할 수 있습니다.</p>
     *
     * @param jobRepository Spring Batch 메타데이터 저장소
     * @param webCrawlingStep 웹 크롤링 Step (Reader → Processor → Writer)
     * @return 실행 가능한 Job 인스턴스
     */
    @Bean
    public Job webCrawlingJob(
            JobRepository jobRepository,
            Step webCrawlingStep) {
        return new JobBuilder("webCrawlingJob", jobRepository)
                .start(webCrawlingStep)
                .build();
    }

    /**
     * 웹 크롤링 Step 정의
     *
     * <p>Chunk-oriented processing 방식으로 동작합니다:</p>
     * <ul>
     *   <li>Chunk Size: 10 (한 번에 10개 아이템씩 처리)</li>
     *   <li>Input Type: String (크롤링 작업 정의 - URL 또는 키워드)</li>
     *   <li>Output Type: Place (장소 엔티티)</li>
     * </ul>
     *
     * <h3>트랜잭션 관리</h3>
     * <p>Chunk 단위로 트랜잭션이 커밋됩니다.
     * 즉, 10개 처리 후 커밋되며, 중간에 실패 시 해당 Chunk만 롤백됩니다.</p>
     *
     * <h3>재사용 가능한 컴포넌트</h3>
     * <p>PlaceDataWriter는 PlaceCollectionJob과 공유하여 사용합니다.
     * 동일한 테이블에 저장하므로 중복 작성이 필요 없습니다.</p>
     *
     * @param jobRepository Spring Batch 메타데이터 저장소
     * @param transactionManager 트랜잭션 관리자
     * @param crawlerTaskReader 크롤링 작업 목록을 읽어오는 Reader (Python 서버 호출)
     * @param crawledDataProcessor 크롤링된 데이터를 Place로 변환하는 Processor
     * @param placeWriter Place를 DB에 저장하는 Writer (PlaceCollectionJob과 공유)
     * @return 실행 가능한 Step 인스턴스
     */
    @Bean
    public Step webCrawlingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<String> crawlerTaskReader,
            ItemProcessor<String, Place> crawledDataProcessor,
            ItemWriter<Place> placeWriter) {
        return new StepBuilder("webCrawlingStep", jobRepository)
                .<String, Place>chunk(10, transactionManager) // 10개씩 chunk 처리
                .reader(crawlerTaskReader)          // Python 크롤러에 작업 요청
                .processor(crawledDataProcessor)    // 크롤링 데이터 변환 및 검증
                .writer(placeWriter)                // DB 저장 (기존 Writer 재사용)
                .build();
    }
}
