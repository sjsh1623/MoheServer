package com.mohe.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mock 위치 설정 Properties.
 *
 * <p>개발/테스트 환경에서 좌표 파라미터가 비어 있을 때 사용할 기본 좌표를 제공한다.
 * 좌표 파라미터가 명시되면 항상 요청 값을 사용하며, 기본 좌표는 요청 값이 누락되었을 때만
 * 사용된다.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "mohe.location")
public class LocationProperties {

    /**
     * Mock 위도.
     * <p>설정되어 있으면 좌표 파라미터가 누락된 요청에서 기본값으로 사용된다.</p>
     */
    private Double defaultLatitude;

    /**
     * Mock 경도.
     * <p>설정되어 있으면 좌표 파라미터가 누락된 요청에서 기본값으로 사용된다.</p>
     */
    private Double defaultLongitude;

    public Double getDefaultLatitude() {
        return defaultLatitude;
    }

    public void setDefaultLatitude(Double defaultLatitude) {
        this.defaultLatitude = defaultLatitude;
    }

    public Double getDefaultLongitude() {
        return defaultLongitude;
    }

    public void setDefaultLongitude(Double defaultLongitude) {
        this.defaultLongitude = defaultLongitude;
    }
}
