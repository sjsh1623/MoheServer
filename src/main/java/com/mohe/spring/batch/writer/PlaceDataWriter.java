package com.mohe.spring.batch.writer;

import com.mohe.spring.entity.Place;
import com.mohe.spring.service.PlaceDataCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * 장소 데이터를 데이터베이스에 저장하는 ItemWriter
 *
 * <p>Processor에서 검증된 Place 엔티티들을 받아
 * 데이터베이스에 영구 저장하는 마지막 단계입니다.</p>
 *
 * <h3>Chunk 처리 방식</h3>
 * <p>Spring Batch는 설정된 chunk 크기(기본 10개)만큼
 * Place를 모아서 한 번에 write() 메서드를 호출합니다.</p>
 *
 * <h3>트랜잭션 관리</h3>
 * <ul>
 *   <li>Chunk 단위로 트랜잭션이 자동 관리됩니다</li>
 *   <li>10개 저장 성공 시 커밋</li>
 *   <li>중간에 실패 시 해당 Chunk 전체 롤백</li>
 *   <li>개별 아이템 저장 실패 시 로그만 남기고 계속 진행</li>
 * </ul>
 *
 * <h3>에러 처리 전략</h3>
 * <p>개별 Place 저장 실패 시에도 배치 전체가 중단되지 않도록
 * try-catch로 감싸서 처리합니다. 실패한 항목은 로그로 기록됩니다.</p>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see org.springframework.batch.item.ItemWriter
 * @see com.mohe.spring.service.PlaceDataCollectionService
 */
@Component
public class PlaceDataWriter implements ItemWriter<Place> {

    private static final Logger logger = LoggerFactory.getLogger(PlaceDataWriter.class);

    /** 장소 데이터 저장을 담당하는 서비스 */
    private final PlaceDataCollectionService placeDataCollectionService;

    /**
     * PlaceDataWriter 생성자
     *
     * @param placeDataCollectionService Place 엔티티 저장 담당 서비스
     */
    public PlaceDataWriter(PlaceDataCollectionService placeDataCollectionService) {
        this.placeDataCollectionService = placeDataCollectionService;
    }

    /**
     * Place 엔티티 리스트를 데이터베이스에 저장 (ItemWriter 인터페이스 구현)
     *
     * <p>Spring Batch가 chunk 크기만큼 모은 Place들을 한 번에 전달하면,
     * 각 Place를 순회하며 데이터베이스에 저장합니다.</p>
     *
     * <h3>처리 흐름</h3>
     * <ol>
     *   <li>Chunk 크기 로깅 (몇 개를 저장할 것인지)</li>
     *   <li>각 Place를 순회하며 savePlace() 호출</li>
     *   <li>저장 성공 시 로그 출력 (장소명, ID)</li>
     *   <li>저장 실패 시 에러 로그만 출력하고 계속 진행</li>
     *   <li>완료 후 총 저장 개수 로깅</li>
     * </ol>
     *
     * <p><b>예시 로그:</b></p>
     * <pre>
     * 💾 Writing 10 places to database
     * ✅ Saved place: 강남 카페거리 (ID: 1234)
     * ✅ Saved place: 홍대 맛집 (ID: 1235)
     * ...
     * ✅ Batch write completed: 10 places saved
     * </pre>
     *
     * <p><b>트랜잭션:</b> 이 메서드가 정상 종료되면 Spring Batch가
     * 자동으로 트랜잭션을 커밋합니다. 예외 발생 시 전체 Chunk가 롤백됩니다.</p>
     *
     * @param chunk 저장할 Place 엔티티 리스트 (Chunk 크기만큼, 기본 10개)
     * @throws Exception write 과정에서 발생할 수 있는 예외
     */
    @Override
    public void write(Chunk<? extends Place> chunk) throws Exception {
        logger.info("💾 Writing {} places to database", chunk.size());

        // Chunk 내 모든 Place를 순회하며 저장
        for (Place place : chunk) {
            try {
                // JPA를 통해 Place 엔티티 저장
                placeDataCollectionService.savePlace(place);
                logger.info("✅ Saved place: {} (ID: {})", place.getName(), place.getId());
            } catch (Exception e) {
                // 개별 저장 실패는 로그만 남기고 계속 진행 (배치 중단 방지)
                logger.error("❌ Failed to save place: {}", place.getName(), e);
            }
        }

        logger.info("✅ Batch write completed: {} places saved", chunk.size());
    }
}
