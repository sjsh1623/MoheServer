package com.mohe.spring.repository;

import com.mohe.spring.entity.DistributedJobLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Distributed Job Lock Repository
 *
 * <p>분산 배치 작업을 위한 락(Lock) 관리 리포지토리</p>
 */
@Repository
public interface DistributedJobLockRepository extends JpaRepository<DistributedJobLock, Long> {

    /**
     * 특정 job과 chunk에 대한 락 조회
     */
    Optional<DistributedJobLock> findByJobNameAndChunkId(String jobName, String chunkId);

    /**
     * 특정 job의 모든 락 조회
     */
    List<DistributedJobLock> findByJobName(String jobName);

    /**
     * 특정 워커가 획득한 모든 락 조회
     */
    List<DistributedJobLock> findByWorkerId(String workerId);

    /**
     * 특정 상태의 락 조회
     */
    List<DistributedJobLock> findByJobNameAndStatus(String jobName, String status);

    /**
     * 만료된 락 조회 (데드 워커 감지)
     */
    @Query("SELECT l FROM DistributedJobLock l WHERE l.jobName = :jobName " +
           "AND l.status IN ('LOCKED', 'PROCESSING') " +
           "AND l.expiresAt < :now")
    List<DistributedJobLock> findExpiredLocks(
        @Param("jobName") String jobName,
        @Param("now") LocalDateTime now
    );

    /**
     * 완료된 락 삭제 (cleanup)
     */
    @Modifying
    @Query("DELETE FROM DistributedJobLock l WHERE l.jobName = :jobName " +
           "AND l.status = 'COMPLETED' " +
           "AND l.completedAt < :before")
    int deleteCompletedLocks(
        @Param("jobName") String jobName,
        @Param("before") LocalDateTime before
    );

    /**
     * 만료된 락을 FAILED로 업데이트
     */
    @Modifying
    @Query("UPDATE DistributedJobLock l SET l.status = 'FAILED', " +
           "l.lastError = 'Lock expired', l.updatedAt = :now " +
           "WHERE l.jobName = :jobName " +
           "AND l.status IN ('LOCKED', 'PROCESSING') " +
           "AND l.expiresAt < :now")
    int markExpiredLocksAsFailed(
        @Param("jobName") String jobName,
        @Param("now") LocalDateTime now
    );

    /**
     * 처리 가능한 청크 조회 (아직 락이 없거나 FAILED 상태)
     */
    @Query(value = "SELECT DISTINCT l.chunk_id FROM distributed_job_lock l " +
           "WHERE l.job_name = :jobName " +
           "AND (l.status = 'FAILED' OR l.expires_at < :now) " +
           "AND l.retry_count < l.max_retries " +
           "LIMIT :limit",
           nativeQuery = true)
    List<String> findRetryableChunks(
        @Param("jobName") String jobName,
        @Param("now") LocalDateTime now,
        @Param("limit") int limit
    );

    /**
     * 락 획득 시도 (atomic operation)
     *
     * @return 성공 시 1, 실패 시 0
     */
    @Modifying
    @Query(value = "INSERT INTO distributed_job_lock " +
           "(job_name, chunk_id, worker_id, worker_hostname, status, " +
           "locked_at, expires_at, retry_count, max_retries, created_at, updated_at) " +
           "VALUES (:jobName, :chunkId, :workerId, :workerHostname, 'LOCKED', " +
           ":lockedAt, :expiresAt, 0, 3, :createdAt, :updatedAt) " +
           "ON CONFLICT (job_name, chunk_id) DO NOTHING",
           nativeQuery = true)
    int tryAcquireLock(
        @Param("jobName") String jobName,
        @Param("chunkId") String chunkId,
        @Param("workerId") String workerId,
        @Param("workerHostname") String workerHostname,
        @Param("lockedAt") LocalDateTime lockedAt,
        @Param("expiresAt") LocalDateTime expiresAt,
        @Param("createdAt") LocalDateTime createdAt,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * 워커의 락 갱신 (heartbeat)
     */
    @Modifying
    @Query("UPDATE DistributedJobLock l SET l.expiresAt = :newExpiresAt, " +
           "l.updatedAt = :now " +
           "WHERE l.jobName = :jobName " +
           "AND l.workerId = :workerId " +
           "AND l.status IN ('LOCKED', 'PROCESSING')")
    int renewLocks(
        @Param("jobName") String jobName,
        @Param("workerId") String workerId,
        @Param("newExpiresAt") LocalDateTime newExpiresAt,
        @Param("now") LocalDateTime now
    );
}
