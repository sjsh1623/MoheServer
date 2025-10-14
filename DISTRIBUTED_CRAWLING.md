# Distributed Crawling Guide

여러 컴퓨터에서 동시에 크롤링 작업을 실행하여 데이터 수집 속도를 높이는 방법을 설명합니다.

## 목차
1. [개요](#개요)
2. [작동 원리](#작동-원리)
3. [사용 방법](#사용-방법)
4. [모니터링](#모니터링)
5. [문제 해결](#문제-해결)

## 개요

### 문제점: 중복 처리
여러 컴퓨터에서 동시에 크롤링하면 같은 데이터를 중복 처리할 수 있습니다:

```
Mac Mini:     Place 1-10, 11-20, 21-30 처리
MacBook Pro:  Place 1-10, 11-20, 21-30 처리  ← 중복!
```

### 해결 방법: 분산 락(Distributed Lock)
데이터베이스 기반 락 메커니즘으로 중복 처리를 방지합니다:

```
Mac Mini:     Place 1-10  (락 획득 ✅), 11-20 (락 획득 ✅)
MacBook Pro:  Place 1-10  (락 실패 ⏭️),  21-30 (락 획득 ✅)
```

각 컴퓨터가 자동으로 **다른 청크**를 처리하므로 중복이 없습니다!

## 작동 원리

### 1. 청크 단위 처리
데이터를 10개씩 청크로 나누어 처리합니다:

```
Chunk 1: Place ID 1-10
Chunk 2: Place ID 11-20
Chunk 3: Place ID 21-30
...
```

### 2. 락(Lock) 획득
각 워커가 청크를 처리하기 전에 락을 획득합니다:

```sql
-- Mac Mini가 Chunk 1에 대한 락 시도
INSERT INTO distributed_job_lock (job_name, chunk_id, worker_id, ...)
VALUES ('distributedCrawlingJob', 'place_1-10', 'mac-mini-abc123', ...)
ON CONFLICT DO NOTHING;

-- 성공 시: 락 획득, 처리 시작
-- 실패 시: 다른 워커가 이미 처리 중, 다음 청크로 이동
```

### 3. 워커 식별
각 컴퓨터는 고유한 Worker ID를 가집니다:

```
Mac Mini:      mac-mini-abc123
MacBook Pro:   macbook-pro-def456
```

### 4. 락 상태 전이

```
LOCKED → PROCESSING → COMPLETED
  ↓
FAILED (에러 발생 시)
```

### 5. 데드 워커(Dead Worker) 감지
컴퓨터가 크래시하면 락이 만료되고, 다른 워커가 인수합니다:

```
Mac Mini 크래시 (10분 타임아웃)
  ↓
락 만료 → FAILED로 표시
  ↓
MacBook Pro가 해당 청크 재처리
```

## 사용 방법

### 1. 데이터베이스 마이그레이션

락 테이블이 자동으로 생성됩니다 (Flyway 마이그레이션):

```sql
-- V99__create_distributed_job_lock.sql
CREATE TABLE distributed_job_lock (
    id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    chunk_id VARCHAR(255) NOT NULL,
    worker_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    ...
);
```

### 2. Mac Mini에서 실행

```bash
cd ~/Developer/Mohe/MoheSpring

# 애플리케이션 시작
./gradlew bootRun

# 또는 Docker Compose
docker compose up app

# 배치 작업 실행 (자동 시작 설정된 경우 생략)
# 수동 실행: POST http://localhost:8080/api/batch/distributed-crawling
```

### 3. MacBook Pro에서 동시 실행

```bash
cd ~/Desktop/Developer/Mohe/MoheSpring

# 동일한 명령 실행
./gradlew bootRun

# 자동으로 다른 청크를 처리합니다!
```

### 4. 로그 확인

**Mac Mini 로그:**
```
🔧 Distributed Place Reader initialized
   Worker: mac-mini-abc123

🔍 Attempting to acquire lock for chunk: place_1-10
🔒 Lock acquired! Processing chunk: place_1-10 (10 places)
📖 Reading place 1/10: 스타벅스 강남점 (id=1)
✅ [mac-mini] Completed: 스타벅스 강남점
...
💾 [mac-mini] Saved 10 places

🔍 Attempting to acquire lock for chunk: place_11-20
⏭️  Skipping chunk: place_11-20 (already locked by another worker)

🔍 Attempting to acquire lock for chunk: place_21-30
🔒 Lock acquired! Processing chunk: place_21-30
```

**MacBook Pro 로그:**
```
🔧 Distributed Place Reader initialized
   Worker: macbook-pro-def456

🔍 Attempting to acquire lock for chunk: place_1-10
⏭️  Skipping chunk: place_1-10 (already locked by another worker)

🔍 Attempting to acquire lock for chunk: place_11-20
🔒 Lock acquired! Processing chunk: place_11-20 (10 places)
📖 Reading place 1/10: 카페 베네 (id=11)
✅ [macbook-pro] Completed: 카페 베네
```

## 모니터링

### 1. 락 상태 조회

```sql
-- 현재 활성 락 조회
SELECT
    chunk_id,
    worker_hostname,
    status,
    locked_at,
    expires_at
FROM distributed_job_lock
WHERE job_name = 'distributedCrawlingJob'
AND status IN ('LOCKED', 'PROCESSING')
ORDER BY locked_at DESC;
```

출력 예시:
```
chunk_id      | worker_hostname | status     | locked_at           | expires_at
--------------|-----------------|------------|---------------------|--------------------
place_21-30   | mac-mini        | PROCESSING | 2025-10-14 12:00:00 | 2025-10-14 12:10:00
place_11-20   | macbook-pro     | PROCESSING | 2025-10-14 12:00:05 | 2025-10-14 12:10:05
```

### 2. 워커별 진행 상황

```sql
-- 각 워커가 처리한 청크 수
SELECT
    worker_hostname,
    COUNT(*) as total_chunks,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
    SUM(CASE WHEN status = 'PROCESSING' THEN 1 ELSE 0 END) as in_progress,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed
FROM distributed_job_lock
WHERE job_name = 'distributedCrawlingJob'
GROUP BY worker_hostname;
```

출력 예시:
```
worker_hostname | total_chunks | completed | in_progress | failed
----------------|--------------|-----------|-------------|-------
mac-mini        | 45           | 43        | 2           | 0
macbook-pro     | 38           | 35        | 3           | 0
```

### 3. 실패한 청크 조회

```sql
-- 에러가 발생한 청크 확인
SELECT
    chunk_id,
    worker_hostname,
    last_error,
    retry_count,
    updated_at
FROM distributed_job_lock
WHERE job_name = 'distributedCrawlingJob'
AND status = 'FAILED'
ORDER BY updated_at DESC;
```

### 4. 만료된 락 확인 (데드 워커)

```sql
-- 타임아웃된 락 조회
SELECT
    chunk_id,
    worker_hostname,
    status,
    expires_at,
    NOW() - expires_at as expired_for
FROM distributed_job_lock
WHERE job_name = 'distributedCrawlingJob'
AND status IN ('LOCKED', 'PROCESSING')
AND expires_at < NOW();
```

## API를 통한 모니터링

### 락 상태 API (추가 구현 필요)

```bash
# 전체 락 상태 조회
curl http://localhost:8080/api/batch/distributed-locks

# 특정 워커의 락 조회
curl http://localhost:8080/api/batch/distributed-locks/my-locks

# 만료된 락 정리
curl -X POST http://localhost:8080/api/batch/distributed-locks/cleanup
```

## 문제 해결

### 1. 워커가 크래시한 경우

워커가 예기치 않게 종료되면 락이 자동으로 만료됩니다:

```sql
-- 만료된 락을 FAILED로 표시 (자동 실행)
UPDATE distributed_job_lock
SET status = 'FAILED', last_error = 'Lock expired'
WHERE job_name = 'distributedCrawlingJob'
AND status IN ('LOCKED', 'PROCESSING')
AND expires_at < NOW();
```

다른 워커가 자동으로 해당 청크를 재처리합니다.

### 2. 모든 워커가 멈춘 경우

수동으로 락을 리셋할 수 있습니다:

```sql
-- 모든 PROCESSING 락을 FAILED로 변경
UPDATE distributed_job_lock
SET status = 'FAILED',
    last_error = 'Manual reset'
WHERE job_name = 'distributedCrawlingJob'
AND status IN ('LOCKED', 'PROCESSING');
```

워커를 다시 시작하면 FAILED 청크를 재처리합니다.

### 3. 특정 청크만 재처리

```sql
-- 특정 청크의 락 삭제 (재처리 허용)
DELETE FROM distributed_job_lock
WHERE job_name = 'distributedCrawlingJob'
AND chunk_id = 'place_21-30';
```

### 4. 완료된 락 정리

오래된 COMPLETED 락을 정리하여 테이블 크기 감소:

```sql
-- 7일 이상 지난 완료된 락 삭제
DELETE FROM distributed_job_lock
WHERE job_name = 'distributedCrawlingJob'
AND status = 'COMPLETED'
AND completed_at < NOW() - INTERVAL '7 days';
```

### 5. 락 타임아웃 조정

일부 장소는 크롤링에 오래 걸릴 수 있습니다. 타임아웃을 조정하세요:

```java
// DistributedPlaceReader.java
lockService.tryAcquireLock(jobName, chunkId, 20); // 20분으로 증가
```

## 성능 최적화

### 1. 청크 크기 조정

```yaml
# application.yml
batch:
  chunk-size: 10  # 기본값
  # 작은 값: 더 세밀한 분산, 락 오버헤드 증가
  # 큰 값: 락 오버헤드 감소, 분산 효율 저하
```

권장 값:
- 빠른 크롤링 (< 1초/place): 20-50
- 일반 크롤링 (1-5초/place): 10-20
- 느린 크롤링 (> 5초/place): 5-10

### 2. 워커 수 조정

최적의 워커 수 = 크롤링 서버 동시 처리 한계 / 청크 크기

예시:
- 크롤링 서버가 동시에 100개 처리 가능
- 청크 크기: 10
- 최적 워커 수: 100 / 10 = 10대

### 3. 네트워크 병목 해결

모든 워커가 같은 크롤링 서버를 사용하면 병목이 발생할 수 있습니다.

해결 방법:
- 크롤링 서버를 여러 대로 확장
- 워커별로 다른 크롤링 서버 URL 지정

```yaml
# Mac Mini: application-mac-mini.yml
crawler:
  server-url: http://crawler-1:5000

# MacBook Pro: application-macbook.yml
crawler:
  server-url: http://crawler-2:5000
```

## 통계

### 처리 속도 비교

**단일 워커:**
- 10개/청크 × 5초/place = 50초/청크
- 1000개 place = 100 청크 × 50초 = 5000초 (83분)

**3대 워커 (분산):**
- 각 워커가 33-34 청크 처리
- 34 청크 × 50초 = 1700초 (28분)
- **속도 3배 향상!**

### 락 오버헤드

각 청크당 락 관련 DB 쿼리:
- 락 획득: INSERT (1회)
- 상태 업데이트: UPDATE (2-3회)
- 완료 처리: UPDATE (1회)

총 오버헤드: **약 10-50ms/청크** (무시할 수 있는 수준)

## 환경 변수 설정

분산 크롤링 관련 환경 변수:

```bash
# .env
# 배치 청크 크기
BATCH_CHUNK_SIZE=10

# 락 타임아웃 (분)
DISTRIBUTED_LOCK_TIMEOUT=10

# 락 정리 주기 (일)
DISTRIBUTED_LOCK_CLEANUP_DAYS=7
```

## 주의사항

1. **데이터베이스 연결**: 모든 워커가 **동일한 PostgreSQL 데이터베이스**에 연결되어야 합니다.
2. **시간 동기화**: 워커 간 시스템 시간이 동기화되어야 합니다 (NTP 사용 권장).
3. **재시도 제한**: 기본 최대 재시도 횟수는 3회입니다.
4. **락 정리**: 주기적으로 완료된 락을 삭제하여 테이블 크기를 관리하세요.

## 관련 파일

- **엔티티**: `DistributedJobLock.java`
- **리포지토리**: `DistributedJobLockRepository.java`
- **서비스**: `DistributedJobLockService.java`
- **리더**: `DistributedPlaceReader.java`
- **작업 설정**: `DistributedCrawlingJobConfig.java`
- **마이그레이션**: `V99__create_distributed_job_lock.sql`

## 참고 자료

- [Spring Batch Documentation](https://docs.spring.io/spring-batch/docs/current/reference/html/)
- [PostgreSQL ON CONFLICT](https://www.postgresql.org/docs/current/sql-insert.html)
- [Distributed Lock Patterns](https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html)
