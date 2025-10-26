package com.mohe.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mock 위치 설정 Properties
 *
 * <p>개발/테스트 환경에서 위치를 강제로 고정할 때 사용하는 Mock 위치 설정입니다.
 *
 * <h3>설정 방법</h3>
 * <ul>
 *   <li><strong>.env 파일</strong>: MOCK_LATITUDE, MOCK_LONGITUDE 환경 변수 설정</li>
 *   <li><strong>application-docker.yml</strong>: mohe.location.default-latitude/longitude 설정</li>
 * </ul>
 *
 * <h3>기본값</h3>
 * <ul>
 *   <li>위도(latitude): null (설정하지 않으면 API 파라미터 사용)</li>
 *   <li>경도(longitude): null (설정하지 않으면 API 파라미터 사용)</li>
 * </ul>
 *
 * <h3>주요 위치 좌표</h3>
 * <pre>
 * 서울 중구:    37.5636, 126.9976 (명동, 시청 일대)
 * 강남역:      37.4979, 127.0276 (강남 상권 중심지)
 * 홍대입구역:  37.5563, 126.9227 (홍대 상권 중심지)
 * 서울역:      37.5547, 126.9707 (서울역 일대)
 * 광화문:      37.5760, 126.9769 (광화문, 종로 일대)
 * </pre>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // .env 파일
 * MOCK_LATITUDE=37.4979   # 강남역으로 고정
 * MOCK_LONGITUDE=127.0276
 *
 * // Controller에서 사용
 * if (locationProperties.getDefaultLatitude() != null) {
 *     // ENV 값 강제 사용
 *     lat = locationProperties.getDefaultLatitude();
 * } else {
 *     // API 파라미터 사용
 *     lat = latitude;
 * }
 * }</pre>
 *
 * @see com.mohe.spring.controller.RecommendationController
 * @see com.mohe.spring.controller.PlaceController
 */
@Configuration
@ConfigurationProperties(prefix = "mohe.location")
public class LocationProperties {

    /**
     * Mock 위도
     * <p>ENV에 설정되어 있으면 해당 값을 강제로 사용합니다.
     * <p>ENV에 설정되어 있지 않으면 (null) API 파라미터를 사용합니다.
     * <p>서울 중구 참고: 37.5636 (명동, 시청 일대)
     * <p>환경 변수: MOCK_LATITUDE
     * <p>설정 키: mohe.location.default-latitude
     */
    private Double defaultLatitude = null;

    /**
     * Mock 경도
     * <p>ENV에 설정되어 있으면 해당 값을 강제로 사용합니다.
     * <p>ENV에 설정되어 있지 않으면 (null) API 파라미터를 사용합니다.
     * <p>서울 중구 참고: 126.9976 (명동, 시청 일대)
     * <p>환경 변수: MOCK_LONGITUDE
     * <p>설정 키: mohe.location.default-longitude
     */
    private Double defaultLongitude = null;

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
