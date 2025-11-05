package com.mohe.spring.service.livemode;

/**
 * Live Mode에서 장소 처리 상태를 나타내는 Enum
 */
public enum ProcessingStatus {
    /**
     * 처리 중 - 크롤링/AI/벡터화가 진행 중인 상태
     */
    IN_PROGRESS,

    /**
     * 처리 완료 - ready=true로 설정 완료
     */
    COMPLETED,

    /**
     * 처리 실패 - 크롤링, AI, 또는 벡터화 중 에러 발생
     */
    FAILED
}
