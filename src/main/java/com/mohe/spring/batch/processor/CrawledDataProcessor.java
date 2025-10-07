package com.mohe.spring.batch.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.entity.Place;
import com.mohe.spring.service.PlaceDataCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Python 크롤링 서버에서 받은 데이터를 Place 엔티티로 변환하는 ItemProcessor
 *
 * <p>Reader에서 받은 크롤링 작업(URL/키워드)을 Python 서버로 전달하고,
 * 응답받은 크롤링 데이터를 검증 및 변환하여 Place 엔티티를 생성합니다.</p>
 *
 * <h3>주요 처리 단계</h3>
 * <ol>
 *   <li><b>Python 서버 호출</b>: HTTP POST 요청으로 크롤링 실행</li>
 *   <li><b>JSON 파싱</b>: 크롤링 결과를 JsonNode로 파싱</li>
 *   <li><b>데이터 검증</b>: 필수 필드 존재 여부 확인</li>
 *   <li><b>Place 엔티티 생성</b>: 크롤링 데이터를 Place 객체로 변환</li>
 *   <li><b>중복 체크</b>: 이미 DB에 존재하는 장소는 스킵</li>
 *   <li><b>추가 정보 보강</b>: Google Places API로 상세 정보 추가 (선택적)</li>
 * </ol>
 *
 * <h3>Python 서버 API 스펙</h3>
 * <pre>
 * POST http://localhost:5000/api/crawl
 * Content-Type: application/json
 *
 * Request Body:
 * {
 *   "task": "서울 카페 추천"  // 크롤링 키워드 또는 URL
 * }
 *
 * Response Body:
 * {
 *   "name": "강남 힐링 카페",
 *   "address": "서울시 강남구 테헤란로 123",
 *   "category": "카페",
 *   "latitude": 37.5012,
 *   "longitude": 127.0396,
 *   "description": "조용하고 아늑한 분위기의 카페",
 *   "imageUrl": "https://example.com/image.jpg"
 * }
 * </pre>
 *
 * <h3>에러 처리</h3>
 * <ul>
 *   <li>Python 서버 연결 실패 → null 반환 (해당 작업 스킵)</li>
 *   <li>JSON 파싱 실패 → null 반환</li>
 *   <li>필수 필드 누락 → null 반환</li>
 *   <li>중복 장소 → null 반환</li>
 * </ul>
 *
 * <p><b>null 반환 시 Writer로 전달되지 않아 해당 아이템은 저장되지 않습니다.</b></p>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see org.springframework.batch.item.ItemProcessor
 * @see com.mohe.spring.service.PlaceDataCollectionService
 */
@Component
public class CrawledDataProcessor implements ItemProcessor<String, Place> {

    private static final Logger logger = LoggerFactory.getLogger(CrawledDataProcessor.class);

    /** HTTP 클라이언트 (Python 서버 호출용) */
    private final RestTemplate restTemplate;

    /** JSON 파싱용 ObjectMapper */
    private final ObjectMapper objectMapper;

    /** Place 데이터 저장 및 검증 담당 서비스 */
    private final PlaceDataCollectionService placeDataCollectionService;

    /**
     * Python 크롤러 서버 URL
     * application.yml에서 주입:
     * batch.crawler.server-url=http://localhost:5000
     */
    @Value("${batch.crawler.server-url:http://localhost:5000}")
    private String pythonServerUrl;

    /**
     * 크롤러 API 엔드포인트
     * application.yml에서 주입:
     * batch.crawler.api-path=/api/crawl
     */
    @Value("${batch.crawler.api-path:/api/crawl}")
    private String crawlerApiPath;

    /**
     * Google Places API로 데이터 보강 여부
     * application.yml에서 주입:
     * batch.crawler.enhance-with-google=true
     */
    @Value("${batch.crawler.enhance-with-google:false}")
    private boolean enhanceWithGoogle;

    /**
     * CrawledDataProcessor 생성자
     *
     * @param restTemplate HTTP 클라이언트
     * @param objectMapper JSON 파싱용
     * @param placeDataCollectionService Place 데이터 처리 서비스
     */
    public CrawledDataProcessor(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            PlaceDataCollectionService placeDataCollectionService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.placeDataCollectionService = placeDataCollectionService;
    }

    /**
     * 크롤링 작업을 Python 서버로 전달하고 결과를 Place 엔티티로 변환 (ItemProcessor 인터페이스 구현)
     *
     * <h3>처리 흐름</h3>
     * <ol>
     *   <li>Python 서버에 크롤링 요청 (HTTP POST)</li>
     *   <li>응답 JSON 파싱</li>
     *   <li>필수 필드 검증 (name, address)</li>
     *   <li>Place 엔티티 생성</li>
     *   <li>중복 체크 (이미 DB에 있으면 스킵)</li>
     *   <li>Google API로 추가 정보 보강 (설정 시)</li>
     * </ol>
     *
     * <h3>반환값</h3>
     * <ul>
     *   <li><b>Place</b>: 성공적으로 변환된 엔티티 → Writer로 전달</li>
     *   <li><b>null</b>: 변환 실패 또는 스킵 → Writer로 전달되지 않음</li>
     * </ul>
     *
     * <h3>예시 로그</h3>
     * <pre>
     * 🕷️ Processing crawler task: 서울 카페 추천
     * 🌐 Calling Python crawler: http://localhost:5000/api/crawl
     * ✅ Crawled place: 강남 힐링 카페 (서울시 강남구 테헤란로 123)
     * 🔍 Enhancing with Google Places API...
     * ✅ Place ready for saving
     * </pre>
     *
     * @param task 크롤링 작업 (URL 또는 키워드)
     * @return 변환된 Place 엔티티 (실패 시 null)
     * @throws Exception 처리 중 발생할 수 있는 예외
     */
    @Override
    public Place process(String task) throws Exception {
        logger.info("🕷️ Processing crawler task: {}", task);

        try {
            // 1. Python 서버에 크롤링 요청
            JsonNode crawledData = callPythonCrawler(task);

            if (crawledData == null) {
                logger.warn("⚠️ Crawler returned no data for task: {}", task);
                return null;
            }

            // 2. 필수 필드 검증
            if (!hasRequiredFields(crawledData)) {
                logger.warn("⚠️ Missing required fields in crawled data");
                return null;
            }

            // 3. Place 엔티티 생성
            Place place = convertToPlace(crawledData);

            // 4. 중복 체크
            if (placeDataCollectionService.isDuplicate(place)) {
                logger.info("⏭️ Duplicate place, skipping: {}", place.getName());
                return null;
            }

            // 5. Google API로 추가 정보 보강 (설정 시)
            if (enhanceWithGoogle) {
                logger.info("🔍 Enhancing with Google Places API...");
                placeDataCollectionService.enhanceWithGooglePlaces(place);
            }

            logger.info("✅ Place ready for saving: {}", place.getName());
            return place;

        } catch (Exception e) {
            logger.error("❌ Failed to process crawler task: {}", task, e);
            return null; // 에러 발생 시 null 반환 (배치 중단 방지)
        }
    }

    /**
     * Python 크롤러 서버에 HTTP 요청을 보내고 JSON 응답을 받습니다
     *
     * @param task 크롤링 작업 (URL 또는 키워드)
     * @return 크롤링 결과 JSON (실패 시 null)
     */
    private JsonNode callPythonCrawler(String task) {
        try {
            String url = pythonServerUrl + crawlerApiPath;
            logger.info("🌐 Calling Python crawler: {}", url);

            // 요청 바디 생성
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("task", task);

            // HTTP POST 요청
            String response = restTemplate.postForObject(url, requestBody, String.class);

            if (response == null || response.isEmpty()) {
                logger.warn("⚠️ Empty response from Python crawler");
                return null;
            }

            // JSON 파싱
            return objectMapper.readTree(response);

        } catch (Exception e) {
            logger.error("❌ Failed to call Python crawler for task: {}", task, e);
            return null;
        }
    }

    /**
     * 크롤링 데이터에 필수 필드가 있는지 검증합니다
     *
     * <p>필수 필드: name</p>
     *
     * @param data 크롤링된 JSON 데이터
     * @return 필수 필드가 모두 있으면 true
     */
    private boolean hasRequiredFields(JsonNode data) {
        return data.has("name") && !data.get("name").asText().isEmpty();
    }

    /**
     * 크롤링 데이터를 Place 엔티티로 변환합니다
     *
     * <h3>매핑 필드</h3>
     * <ul>
     *   <li>name → Place.name</li>
     *   <li>category → Place.category</li>
     *   <li>latitude → Place.latitude</li>
     *   <li>longitude → Place.longitude</li>
     *   <li>description → Place.description</li>
     * </ul>
     *
     * @param data 크롤링된 JSON 데이터
     * @return 변환된 Place 엔티티
     */
    private Place convertToPlace(JsonNode data) {
        Place place = new Place();

        // 필수 필드
        place.setName(data.get("name").asText());

        // 선택 필드 (없으면 기본값 사용)
        if (data.has("category")) {
            place.setCategory(data.get("category").asText());
        }

        if (data.has("latitude") && data.has("longitude")) {
            place.setLatitude(BigDecimal.valueOf(data.get("latitude").asDouble()));
            place.setLongitude(BigDecimal.valueOf(data.get("longitude").asDouble()));
        }

        if (data.has("description")) {
            place.setDescription(data.get("description").asText());
        }

        logger.info("✅ Crawled place: {}", place.getName());
        return place;
    }
}
