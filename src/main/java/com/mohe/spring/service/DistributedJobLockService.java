package com.mohe.spring.service;

import com.mohe.spring.entity.DistributedJobLock;
import com.mohe.spring.repository.DistributedJobLockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Distributed Job Lock Service
 *
 * <p>여러 컴퓨터에서 동시에 배치 작업을 실행할 때
 * 중복 처리를 방지하기 위한 락(Lock) 서비스입니다.</p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>청크 단위 락 획득 (atomic operation)</li>
 *   <li>만료된 락 자동 감지 및 복구</li>
 *   <li>워커 heartbeat를 통한 락 갱신</li>
 *   <li>완료된 락 정리 (cleanup)</li>
 * </ul>
 */
@Service
public class DistributedJobLockService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedJobLockService.class);

    private final DistributedJobLockRepository lockRepository;
    private final String workerId;
    private final String workerHostname;

    /**
     * 기본 락 타임아웃 (10분)
     */
    private static final int DEFAULT_LOCK_TIMEOUT_MINUTES = 10;

    public DistributedJobLockService(DistributedJobLockRepository lockRepository) {
        this.lockRepository = lockRepository;
        this.workerId = generateWorkerId();
        this.workerHostname = getHostname();

        logger.info("🔧 Distributed Job Lock Service initialized");
        logger.info("   Worker ID: {}", workerId);
        logger.info("   Hostname: {}", workerHostname);
    }

    /**
     * 워커 ID 생성 (hostname + UUID)
     */
    private String generateWorkerId() {
        String hostname = getHostname();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return hostname + "-" + uuid;
    }

    /**
     * 호스트네임 조회
     */
    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            logger.warn("Failed to get hostname, using 'unknown'", e);
            return "unknown";
        }
    }

    /**
     * 청크 락 획득 시도
     *
     * @param jobName  작업 이름
     * @param chunkId  청크 식별자 (예: "place_1-10")
     * @return 락 획득 성공 여부
     */
    @Transactional
    public boolean tryAcquireLock(String jobName, String chunkId) {
        return tryAcquireLock(jobName, chunkId, DEFAULT_LOCK_TIMEOUT_MINUTES);
    }

    /**
     * 청크 락 획득 시도 (타임아웃 지정)
     *
     * @param jobName         작업 이름
     * @param chunkId         청크 식별자
     * @param timeoutMinutes  락 타임아웃 (분)
     * @return 락 획득 성공 여부
     */
    @Transactional
    public boolean tryAcquireLock(String jobName, String chunkId, int timeoutMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(timeoutMinutes);

        try {
            int rowsAffected = lockRepository.tryAcquireLock(
                jobName,
                chunkId,
                workerId,
                workerHostname,
                now,
                expiresAt,
                now,
                now
            );

            if (rowsAffected > 0) {
                logger.info("🔒 Lock acquired: job={}, chunk={}, worker={}",
                    jobName, chunkId, workerId);
                return true;
            } else {
                logger.debug("⏳ Lock already held: job={}, chunk={}", jobName, chunkId);
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Failed to acquire lock: job={}, chunk={}", jobName, chunkId, e);
            return false;
        }
    }

    /**
     * 락 상태 업데이트 (LOCKED → PROCESSING)
     *
     * @param jobName  작업 이름
     * @param chunkId  청크 식별자
     */
    @Transactional
    public void markAsProcessing(String jobName, String chunkId) {
        lockRepository.findByJobNameAndChunkId(jobName, chunkId).ifPresent(lock -> {
            if (lock.getWorkerId().equals(workerId)) {
                lock.setStatus("PROCESSING");
                lock.setStartedAt(LocalDateTime.now());
                lockRepository.save(lock);
                logger.info("▶️ Lock status: PROCESSING - job={}, chunk={}", jobName, chunkId);
            }
        });
    }

    /**
     * 락 완료 처리 (PROCESSING → COMPLETED)
     *
     * @param jobName  작업 이름
     * @param chunkId  청크 식별자
     */
    @Transactional
    public void markAsCompleted(String jobName, String chunkId) {
        lockRepository.findByJobNameAndChunkId(jobName, chunkId).ifPresent(lock -> {
            if (lock.getWorkerId().equals(workerId)) {
                lock.setStatus("COMPLETED");
                lock.setCompletedAt(LocalDateTime.now());
                lockRepository.save(lock);
                logger.info("✅ Lock completed: job={}, chunk={}", jobName, chunkId);
            }
        });
    }

    /**
     * 락 실패 처리 (PROCESSING → FAILED)
     *
     * @param jobName  작업 이름
     * @param chunkId  청크 식별자
     * @param error    에러 메시지
     */
    @Transactional
    public void markAsFailed(String jobName, String chunkId, String error) {
        lockRepository.findByJobNameAndChunkId(jobName, chunkId).ifPresent(lock -> {
            if (lock.getWorkerId().equals(workerId)) {
                lock.setStatus("FAILED");
                lock.setLastError(error);
                lock.setRetryCount(lock.getRetryCount() + 1);
                lockRepository.save(lock);
                logger.error("❌ Lock failed: job={}, chunk={}, error={}", jobName, chunkId, error);
            }
        });
    }

    /**
     * 워커의 모든 락 갱신 (heartbeat)
     *
     * @param jobName          작업 이름
     * @param extensionMinutes 연장 시간 (분)
     * @return 갱신된 락 개수
     */
    @Transactional
    public int renewLocks(String jobName, int extensionMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiresAt = now.plusMinutes(extensionMinutes);

        int renewed = lockRepository.renewLocks(jobName, workerId, newExpiresAt, now);
        if (renewed > 0) {
            logger.debug("💓 Heartbeat: renewed {} locks for worker {}", renewed, workerId);
        }
        return renewed;
    }

    /**
     * 만료된 락을 FAILED로 표시
     *
     * @param jobName 작업 이름
     * @return 업데이트된 락 개수
     */
    @Transactional
    public int markExpiredLocksAsFailed(String jobName) {
        LocalDateTime now = LocalDateTime.now();
        int marked = lockRepository.markExpiredLocksAsFailed(jobName, now);
        if (marked > 0) {
            logger.warn("⏰ Marked {} expired locks as FAILED for job {}", marked, jobName);
        }
        return marked;
    }

    /**
     * 완료된 락 정리 (N일 이상 지난 것)
     *
     * @param jobName 작업 이름
     * @param daysAgo 보관 기간 (일)
     * @return 삭제된 락 개수
     */
    @Transactional
    public int cleanupCompletedLocks(String jobName, int daysAgo) {
        LocalDateTime before = LocalDateTime.now().minusDays(daysAgo);
        int deleted = lockRepository.deleteCompletedLocks(jobName, before);
        if (deleted > 0) {
            logger.info("🧹 Cleaned up {} completed locks older than {} days", deleted, daysAgo);
        }
        return deleted;
    }

    /**
     * 특정 작업의 모든 락 조회
     *
     * @param jobName 작업 이름
     * @return 락 목록
     */
    public List<DistributedJobLock> getLocks(String jobName) {
        return lockRepository.findByJobName(jobName);
    }

    /**
     * 현재 워커가 보유한 락 조회
     *
     * @return 락 목록
     */
    public List<DistributedJobLock> getMyLocks() {
        return lockRepository.findByWorkerId(workerId);
    }

    /**
     * 특정 청크의 락 조회
     *
     * @param jobName 작업 이름
     * @param chunkId 청크 식별자
     * @return 락 (Optional)
     */
    public Optional<DistributedJobLock> getLock(String jobName, String chunkId) {
        return lockRepository.findByJobNameAndChunkId(jobName, chunkId);
    }

    /**
     * 재시도 가능한 청크 조회
     *
     * @param jobName 작업 이름
     * @param limit   최대 개수
     * @return 청크 ID 목록
     */
    public List<String> getRetryableChunks(String jobName, int limit) {
        LocalDateTime now = LocalDateTime.now();
        return lockRepository.findRetryableChunks(jobName, now, limit);
    }

    /**
     * 현재 워커 ID 조회
     */
    public String getWorkerId() {
        return workerId;
    }

    /**
     * 워커 호스트네임 조회
     */
    public String getWorkerHostname() {
        return workerHostname;
    }
}
