package com.mohe.spring.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 기본 설정
 *
 * Spring Batch 5.x (Spring Boot 3.x)에서는 @EnableBatchProcessing을 사용하되
 * DefaultBatchConfiguration을 상속받아 기본 설정을 제공합니다.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration extends DefaultBatchConfiguration {

    // Spring Batch 메타데이터 테이블은 자동으로 생성됩니다
    // BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION, BATCH_STEP_EXECUTION 등

    // 커스텀 설정이 필요한 경우 여기에 추가
}
