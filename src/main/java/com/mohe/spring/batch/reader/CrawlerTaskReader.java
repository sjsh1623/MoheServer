package com.mohe.spring.batch.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Python 크롤링 서버에 작업을 요청하는 ItemReader
 *
 * <p>크롤링할 URL 목록 또는 키워드를 생성하여 Processor에 전달합니다.
 * Processor에서 실제 Python 서버 HTTP 호출이 이루어집니다.</p>
 *
 * <h3>주요 역할</h3>
 * <ul>
 *   <li>크롤링 대상 목록 생성 (URL 또는 키워드)</li>
 *   <li>순차적으로 하나씩 작업 아이템 반환</li>
 *   <li>모든 작업 완료 시 null 반환하여 종료 신호</li>
 * </ul>
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li>초기화: 크롤링 대상 목록 생성 (URL 또는 검색 키워드)</li>
 *   <li>read() 호출 시마다 하나씩 반환</li>
 *   <li>모든 항목 소진 시 null 반환 → Batch 종료</li>
 * </ol>
 *
 * <h3>크롤링 작업 예시</h3>
 * <pre>
 * 1. URL 기반 크롤링:
 *    - "https://example.com/places/seoul"
 *    - "https://example.com/places/busan"
 *
 * 2. 키워드 기반 크롤링:
 *    - "서울 카페 추천"
 *    - "부산 맛집 리스트"
 * </pre>
 *
 * <h3>Python 서버 연동</h3>
 * <p>이 Reader는 작업 목록만 생성합니다.
 * 실제 Python 서버 호출은 {@link com.mohe.spring.batch.processor.CrawledDataProcessor}에서 수행됩니다.</p>
 *
 * <h3>설정 방법</h3>
 * <p>application.yml에서 크롤링 대상을 설정할 수 있습니다:</p>
 * <pre>
 * batch:
 *   crawler:
 *     targets:
 *       - "서울 카페"
 *       - "부산 맛집"
 *       - "제주 관광지"
 * </pre>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see org.springframework.batch.item.ItemReader
 * @see com.mohe.spring.batch.processor.CrawledDataProcessor
 */
@Component
public class CrawlerTaskReader implements ItemReader<String> {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerTaskReader.class);

    /**
     * 크롤링 작업 목록
     * - URL 또는 키워드 리스트
     * - 한 번만 초기화되며 read() 호출 시마다 하나씩 반환
     */
    private List<String> crawlerTasks;

    /**
     * 현재 읽고 있는 인덱스
     * - read() 호출 시마다 증가
     * - crawlerTasks.size()에 도달하면 null 반환
     */
    private int currentIndex = 0;

    /**
     * Python 크롤러 서버 URL
     * application.yml에서 주입:
     * batch.crawler.server-url=http://localhost:5000
     */
    @Value("${batch.crawler.server-url:http://localhost:5000}")
    private String pythonServerUrl;

    /**
     * 크롤링 대상 키워드/URL 목록 (기본값)
     * application.yml에서 주입 가능:
     * batch.crawler.targets[0]=서울 카페
     * batch.crawler.targets[1]=부산 맛집
     */
    @Value("${batch.crawler.targets:}")
    private String[] crawlerTargets;

    /**
     * 기본 크롤링 카테고리 (설정 파일에 없을 경우 사용)
     * - 다양한 장소 유형을 크롤링하기 위한 키워드
     */
    private static final List<String> DEFAULT_CRAWLER_CATEGORIES = Arrays.asList(
            "서울 카페 추천",
            "부산 맛집",
            "제주 관광지",
            "경주 역사 유적지",
            "강릉 해변",
            "전주 한옥마을",
            "여수 밤바다",
            "대구 동성로",
            "인천 차이나타운",
            "수원 화성"
    );

    /**
     * 크롤링 작업 아이템을 하나씩 읽어옵니다 (ItemReader 인터페이스 구현)
     *
     * <p>Spring Batch가 반복적으로 이 메서드를 호출하여
     * 크롤링할 대상을 하나씩 가져갑니다.</p>
     *
     * <h3>처리 흐름</h3>
     * <ol>
     *   <li>첫 호출 시: 크롤링 작업 목록 초기화</li>
     *   <li>현재 인덱스의 작업 반환</li>
     *   <li>인덱스 증가</li>
     *   <li>모든 작업 완료 시: null 반환 (Batch 종료 신호)</li>
     * </ol>
     *
     * <h3>반환값</h3>
     * <ul>
     *   <li><b>String</b>: 크롤링 작업 (URL 또는 키워드)</li>
     *   <li><b>null</b>: 모든 작업 완료 (Spring Batch가 Step 종료)</li>
     * </ul>
     *
     * <h3>예시 로그</h3>
     * <pre>
     * 🕷️ Initializing crawler tasks: 10 targets
     * 📋 Reading crawler task [1/10]: 서울 카페 추천
     * 📋 Reading crawler task [2/10]: 부산 맛집
     * ...
     * ✅ All crawler tasks completed
     * </pre>
     *
     * @return 크롤링 작업 (URL 또는 키워드), 모든 작업 완료 시 null
     * @throws Exception 읽기 중 발생할 수 있는 예외
     */
    @Override
    public String read() throws Exception {
        // 1. 첫 호출 시 크롤링 작업 목록 초기화
        if (crawlerTasks == null) {
            initializeCrawlerTasks();
        }

        // 2. 모든 작업 완료 시 null 반환 (Batch 종료 신호)
        if (currentIndex >= crawlerTasks.size()) {
            logger.info("✅ All crawler tasks completed");
            return null;
        }

        // 3. 현재 인덱스의 작업 반환
        String task = crawlerTasks.get(currentIndex);
        logger.info("📋 Reading crawler task [{}/{}]: {}",
                currentIndex + 1, crawlerTasks.size(), task);

        // 4. 다음 작업을 위해 인덱스 증가
        currentIndex++;

        return task;
    }

    /**
     * 크롤링 작업 목록 초기화 (내부 헬퍼 메서드)
     *
     * <p>application.yml에서 설정된 대상이 있으면 사용하고,
     * 없으면 기본 카테고리 리스트를 사용합니다.</p>
     *
     * <h3>초기화 우선순위</h3>
     * <ol>
     *   <li>application.yml 설정 (batch.crawler.targets)</li>
     *   <li>기본 카테고리 리스트 (DEFAULT_CRAWLER_CATEGORIES)</li>
     * </ol>
     */
    private void initializeCrawlerTasks() {
        crawlerTasks = new ArrayList<>();

        // 1. application.yml에서 설정된 대상이 있으면 사용
        if (crawlerTargets != null && crawlerTargets.length > 0) {
            crawlerTasks.addAll(Arrays.asList(crawlerTargets));
            logger.info("🕷️ Initializing crawler tasks from config: {} targets", crawlerTasks.size());
        } else {
            // 2. 설정 파일이 없으면 기본 카테고리 사용
            crawlerTasks.addAll(DEFAULT_CRAWLER_CATEGORIES);
            logger.info("🕷️ Initializing crawler tasks from defaults: {} targets", crawlerTasks.size());
        }

        logger.info("🔗 Python crawler server: {}", pythonServerUrl);
    }

    /**
     * Python 서버 URL 반환 (Processor에서 사용)
     *
     * @return Python 크롤러 서버 URL
     */
    public String getPythonServerUrl() {
        return pythonServerUrl;
    }
}
