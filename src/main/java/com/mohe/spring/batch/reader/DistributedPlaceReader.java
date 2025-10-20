package com.mohe.spring.batch.reader;

import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.DistributedJobLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Distributed Place Reader with Lock Mechanism
 *
 * <p>여러 컴퓨터에서 동시에 크롤링 작업을 실행할 때
 * 중복 처리를 방지하는 Reader입니다.</p>
 *
 * <h3>작동 방식</h3>
 * <ol>
 *   <li>DB에서 10개씩 Place 조회</li>
 *   <li>해당 청크에 대한 락 획득 시도</li>
 *   <li>락 획득 성공 시 처리, 실패 시 다음 청크로 이동</li>
 *   <li>다른 워커가 이미 처리 중이면 자동으로 건너뛰기</li>
 * </ol>
 *
 * <h3>사용 예시</h3>
 * <pre>
 * // Mac Mini: chunk 1-10, 11-20 처리
 * // MacBook Pro: chunk 21-30, 31-40 처리
 * // 겹치지 않음!
 * </pre>
 */
public class DistributedPlaceReader implements ItemReader<Place> {

    private static final Logger logger = LoggerFactory.getLogger(DistributedPlaceReader.class);

    private final PlaceRepository placeRepository;
    private final DistributedJobLockService lockService;
    private final String jobName;
    private final int chunkSize;

    private int currentPage = 0;
    private List<Place> currentChunk = new ArrayList<>();
    private int currentIndex = 0;
    private String currentChunkId = null;

    /**
     * Constructor
     *
     * @param placeRepository Place repository
     * @param lockService     Lock service
     * @param jobName         Job name (e.g., "updateCrawledDataJob")
     * @param chunkSize       Chunk size (e.g., 10)
     */
    public DistributedPlaceReader(
        PlaceRepository placeRepository,
        DistributedJobLockService lockService,
        String jobName,
        int chunkSize
    ) {
        this.placeRepository = placeRepository;
        this.lockService = lockService;
        this.jobName = jobName;
        this.chunkSize = chunkSize;

        logger.info("🔧 Distributed Place Reader initialized");
        logger.info("   Job: {}", jobName);
        logger.info("   Chunk size: {}", chunkSize);
        logger.info("   Worker: {}", lockService.getWorkerId());
    }

    /**
     * Read next Place
     *
     * <p>현재 청크에서 Place를 순차적으로 반환하고,
     * 청크가 끝나면 다음 청크를 로드합니다.</p>
     *
     * @return 다음 Place, 없으면 null
     */
    @Override
    public Place read() throws Exception {
        // 현재 청크에 아직 Place가 남아있으면 반환
        if (currentIndex < currentChunk.size()) {
            Place place = currentChunk.get(currentIndex);
            currentIndex++;
            logger.debug("📖 Reading place {}/{}: {} (id={})",
                currentIndex, currentChunk.size(), place.getName(), place.getId());
            return place;
        }

        // 현재 청크가 끝났으면 완료 처리
        if (currentChunkId != null) {
            lockService.markAsCompleted(jobName, currentChunkId);
            currentChunkId = null;
        }

        // 다음 청크 로드 시도
        while (true) {
            // 만료된 락 정리 (매 청크마다 체크)
            lockService.markExpiredLocksAsFailed(jobName);

            // 다음 페이지의 Place ID 조회
            Page<Long> idsPage = placeRepository.findPlaceIdsForBatchProcessing(
                PageRequest.of(currentPage, chunkSize, Sort.by("id").ascending())
            );

            if (idsPage.isEmpty()) {
                logger.info("✅ No more places to process");
                return null;
            }

            List<Long> placeIds = idsPage.getContent();
            Long firstId = placeIds.get(0);
            Long lastId = placeIds.get(placeIds.size() - 1);
            String chunkId = String.format("place_%d-%d", firstId, lastId);

            logger.info("🔍 Attempting to acquire lock for chunk: {} (page {}, {} place IDs)",
                chunkId, currentPage, placeIds.size());

            // 락 획득 시도
            boolean lockAcquired = lockService.tryAcquireLock(jobName, chunkId);

            if (lockAcquired) {
                // 락 획득 성공 - Place 엔티티 로드 (컬렉션 포함)
                List<Place> places = new ArrayList<>();
                for (Long id : placeIds) {
                    placeRepository.findByIdWithCollections(id).ifPresent(place -> {
                        // Force-load other collections to avoid LazyInitializationException
                        place.getImages().size();
                        place.getBusinessHours().size();
                        place.getSns().size();
                        place.getReviews().size();
                        places.add(place);
                    });
                }

                currentChunk = places;
                currentIndex = 0;
                currentChunkId = chunkId;

                lockService.markAsProcessing(jobName, chunkId);

                logger.info("🔒 Lock acquired! Processing chunk: {} ({} places loaded)",
                    chunkId, places.size());

                // 첫 번째 Place 반환
                Place first = currentChunk.get(currentIndex);
                currentIndex++;
                return first;
            } else {
                // 락 획득 실패 - 다른 워커가 처리 중
                logger.info("⏭️  Skipping chunk: {} (already locked by another worker)",
                    chunkId);
                currentPage++;

                // 무한 루프 방지: 최대 100페이지까지만 시도
                if (currentPage > 100) {
                    logger.warn("⚠️  Reached maximum page limit (100), stopping");
                    return null;
                }
            }
        }
    }

    /**
     * 현재 워커의 락 갱신 (heartbeat)
     *
     * <p>장시간 실행되는 작업의 경우 주기적으로 호출해야 합니다.</p>
     */
    public void renewLocks() {
        lockService.renewLocks(jobName, 10);
    }

    /**
     * 에러 발생 시 현재 청크를 FAILED로 표시
     *
     * @param error 에러 메시지
     */
    public void markCurrentChunkAsFailed(String error) {
        if (currentChunkId != null) {
            lockService.markAsFailed(jobName, currentChunkId, error);
            currentChunkId = null;
        }
    }
}
