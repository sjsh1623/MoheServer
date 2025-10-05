package com.mohe.spring.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 기본 설정
 *
 * <p>Spring Batch 5.x (Spring Boot 3.x) 방식으로 배치 인프라를 구성합니다.
 * {@link DefaultBatchConfiguration}을 상속받아 기본 설정을 제공하며,
 * {@code @EnableBatchProcessing} 어노테이션으로 배치 기능을 활성화합니다.</p>
 *
 * <h3>자동 생성되는 메타데이터 테이블</h3>
 * <p>Spring Batch는 실행 이력을 추적하기 위해 다음 테이블을 자동 생성합니다:</p>
 * <ul>
 *   <li><b>BATCH_JOB_INSTANCE</b>: Job의 고유한 실행 인스턴스</li>
 *   <li><b>BATCH_JOB_EXECUTION</b>: Job 실행 이력 (시작/종료 시간, 상태)</li>
 *   <li><b>BATCH_STEP_EXECUTION</b>: Step 실행 이력 (읽기/쓰기/스킵 개수)</li>
 *   <li><b>BATCH_JOB_EXECUTION_PARAMS</b>: Job 실행 파라미터</li>
 *   <li><b>BATCH_JOB_EXECUTION_CONTEXT</b>: Job 실행 컨텍스트</li>
 *   <li><b>BATCH_STEP_EXECUTION_CONTEXT</b>: Step 실행 컨텍스트</li>
 * </ul>
 *
 * <h3>application.yml 설정</h3>
 * <pre>
 * spring:
 *   batch:
 *     job:
 *       enabled: false  # 자동 실행 방지 (수동 트리거)
 *     jdbc:
 *       initialize-schema: always  # 메타데이터 테이블 자동 생성
 * </pre>
 *
 * <h3>커스터마이징</h3>
 * <p>필요 시 다음 메서드를 오버라이드하여 설정을 변경할 수 있습니다:</p>
 * <ul>
 *   <li>{@code getTransactionManager()}: 트랜잭션 매니저 변경</li>
 *   <li>{@code getDataSource()}: 데이터소스 변경</li>
 *   <li>{@code getBatchDataSource()}: 메타데이터 전용 DB 분리</li>
 * </ul>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see org.springframework.batch.core.configuration.support.DefaultBatchConfiguration
 * @see org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration extends DefaultBatchConfiguration {

    // ✅ Spring Batch 메타데이터 테이블은 자동으로 생성됩니다
    // - BATCH_JOB_INSTANCE: Job 인스턴스
    // - BATCH_JOB_EXECUTION: Job 실행 이력
    // - BATCH_STEP_EXECUTION: Step 실행 이력
    // - BATCH_JOB_EXECUTION_PARAMS: Job 파라미터
    // - BATCH_JOB_EXECUTION_CONTEXT: Job 컨텍스트
    // - BATCH_STEP_EXECUTION_CONTEXT: Step 컨텍스트

    // 💡 커스텀 설정이 필요한 경우 아래 메서드를 오버라이드하세요
    // @Override
    // public PlatformTransactionManager getTransactionManager() { ... }
    //
    // @Override
    // public DataSource getDataSource() { ... }
}
