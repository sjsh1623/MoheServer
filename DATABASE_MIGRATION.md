# Database Migration Guide

이 문서는 Mohe 프로젝트의 PostgreSQL 데이터베이스를 다른 머신(예: Mac Mini)으로 이동하는 방법을 설명합니다.

## 목차
1. [개요](#개요)
2. [방법 1: 스크립트 사용 (권장)](#방법-1-스크립트-사용-권장)
3. [방법 2: 수동 백업/복원](#방법-2-수동-백업복원)
4. [방법 3: Docker Volume 직접 복사](#방법-3-docker-volume-직접-복사)
5. [문제 해결](#문제-해결)

## 개요

Mohe 프로젝트는 PostgreSQL 15 데이터베이스를 사용하며, Docker Compose로 관리됩니다.

**데이터베이스 정보:**
- Database: `mohe_db`
- User: `mohe_user`
- Docker Volume: `postgres_data`
- Container: `mohe-postgres`

## 방법 1: 스크립트 사용 (권장)

가장 간단하고 안전한 방법입니다.

### 단계 1: 현재 머신에서 백업 생성

```bash
# 백업 스크립트 실행
./scripts/db-backup.sh

# 백업 파일 위치 확인
ls -lh db-backups/
```

출력 예시:
```
mohe_backup_20250114_153000.sql      (3.2 MB)
latest.sql -> mohe_backup_20250114_153000.sql
```

**옵션: 백업 압축**

스크립트 실행 중 압축 여부를 물으면 `y`를 입력하면 gzip으로 압축됩니다:
```
mohe_backup_20250114_153000.sql.gz   (800 KB)
```

### 단계 2: Mac Mini로 백업 파일 전송

**방법 A: SCP 사용**
```bash
# Mac Mini로 파일 복사
scp db-backups/mohe_backup_*.sql username@mac-mini-ip:~/

# 또는 압축 파일 전송 (더 빠름)
scp db-backups/mohe_backup_*.sql.gz username@mac-mini-ip:~/
```

**방법 B: USB 드라이브 사용**
```bash
# USB에 복사
cp db-backups/mohe_backup_*.sql /Volumes/USB/

# Mac Mini에서 프로젝트 폴더로 복사
cp ~/mohe_backup_*.sql ~/Developer/Mohe/MoheSpring/db-backups/
```

**방법 C: AirDrop 사용 (macOS)**
- Finder에서 `db-backups` 폴더를 열고
- 백업 파일을 Mac Mini로 AirDrop

### 단계 3: Mac Mini에서 복원

```bash
# Mac Mini에서 프로젝트 디렉토리로 이동
cd ~/Developer/Mohe/MoheSpring

# Docker Compose로 PostgreSQL 시작 (아직 시작 안 했다면)
docker compose up -d postgres

# 데이터베이스 복원
./scripts/db-restore.sh mohe_backup_20250114_153000.sql

# 또는 압축 파일로 직접 복원 (자동으로 압축 해제)
./scripts/db-restore.sh mohe_backup_20250114_153000.sql.gz

# 또는 latest 백업 사용
./scripts/db-restore.sh
```

### 단계 4: 검증

```bash
# 데이터베이스 접속하여 확인
docker exec -it mohe-postgres psql -U mohe_user -d mohe_db

# SQL 프롬프트에서
\dt              # 테이블 목록 확인
SELECT COUNT(*) FROM places;     # 장소 데이터 확인
SELECT COUNT(*) FROM users;      # 사용자 데이터 확인
\q               # 종료

# 애플리케이션 시작
docker compose up -d app

# API 테스트
curl http://localhost:8080/health
```

## 방법 2: 수동 백업/복원

스크립트를 사용하지 않고 수동으로 처리하는 방법입니다.

### 현재 머신에서 백업

```bash
# 백업 디렉토리 생성
mkdir -p db-backups

# pg_dump로 백업
docker exec -t mohe-postgres pg_dump \
  -U mohe_user \
  -d mohe_db \
  --clean \
  --if-exists \
  --create \
  > db-backups/manual_backup.sql

# 압축 (선택사항)
gzip db-backups/manual_backup.sql
```

### Mac Mini에서 복원

```bash
# 애플리케이션 컨테이너 중지
docker compose stop app

# 데이터베이스 복원
docker exec -i mohe-postgres psql \
  -U mohe_user \
  -d postgres \
  < ~/manual_backup.sql

# 또는 압축 파일
gunzip -c ~/manual_backup.sql.gz | docker exec -i mohe-postgres psql \
  -U mohe_user \
  -d postgres

# 애플리케이션 재시작
docker compose start app
```

## 방법 3: Docker Volume 직접 복사

전체 Docker Volume을 복사하는 방법입니다. (고급 사용자용)

### 현재 머신에서 Volume 백업

```bash
# Volume을 tar로 백업
docker run --rm \
  -v mohespring_postgres_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/postgres_volume_backup.tar.gz -C /data .

# 백업 파일 확인
ls -lh postgres_volume_backup.tar.gz
```

### Mac Mini로 전송 및 복원

```bash
# 1. Mac Mini로 파일 전송
scp postgres_volume_backup.tar.gz username@mac-mini-ip:~/

# 2. Mac Mini에서 복원
cd ~/Developer/Mohe/MoheSpring

# Docker Compose로 Volume 생성 (비어 있는 상태)
docker compose up -d postgres
docker compose stop postgres

# Volume에 데이터 복원
docker run --rm \
  -v mohespring_postgres_data:/data \
  -v $(pwd):/backup \
  alpine sh -c "cd /data && tar xzf /backup/postgres_volume_backup.tar.gz"

# PostgreSQL 시작
docker compose up -d postgres

# 검증
docker exec -it mohe-postgres psql -U mohe_user -d mohe_db -c '\dt'
```

## 네트워크를 통한 실시간 전송

로컬 네트워크에서 직접 전송하는 방법입니다.

### Mac Mini에서 수신 대기

```bash
# Mac Mini에서 (포트 5555로 수신)
cd ~/Developer/Mohe/MoheSpring
nc -l 5555 | gunzip | docker exec -i mohe-postgres psql -U mohe_user -d postgres
```

### 현재 머신에서 전송

```bash
# 현재 머신에서 (Mac Mini IP: 192.168.1.100)
docker exec -t mohe-postgres pg_dump \
  -U mohe_user \
  -d mohe_db \
  --clean --if-exists --create | gzip | nc 192.168.1.100 5555
```

## 문제 해결

### 1. "role does not exist" 에러

데이터베이스 사용자가 없는 경우:

```bash
# 사용자 생성
docker exec -it mohe-postgres psql -U postgres -c \
  "CREATE USER mohe_user WITH PASSWORD 'mohe_password';"

docker exec -it mohe-postgres psql -U postgres -c \
  "ALTER USER mohe_user CREATEDB;"
```

### 2. "database already exists" 에러

기존 데이터베이스 삭제 후 복원:

```bash
# 애플리케이션 중지
docker compose stop app

# 데이터베이스 삭제 및 재생성
docker exec -it mohe-postgres psql -U postgres -c "DROP DATABASE IF EXISTS mohe_db;"
docker exec -it mohe-postgres psql -U postgres -c "CREATE DATABASE mohe_db OWNER mohe_user;"

# 백업 복원
./scripts/db-restore.sh
```

### 3. 권한 문제

```bash
# 스크립트 실행 권한 부여
chmod +x scripts/db-backup.sh
chmod +x scripts/db-restore.sh

# 백업 디렉토리 권한 확인
ls -la db-backups/
```

### 4. 용량 부족

대용량 데이터베이스의 경우:

```bash
# 압축하여 백업 (용량 약 70% 감소)
docker exec -t mohe-postgres pg_dump \
  -U mohe_user -d mohe_db \
  --clean --if-exists --create | gzip > backup.sql.gz

# 특정 테이블만 백업 (예: places 테이블)
docker exec -t mohe-postgres pg_dump \
  -U mohe_user -d mohe_db \
  -t places -t users | gzip > partial_backup.sql.gz
```

### 5. Docker Volume이 없는 경우

```bash
# Volume 목록 확인
docker volume ls | grep postgres

# Volume 생성
docker volume create mohespring_postgres_data

# docker-compose.yml 확인
cat docker-compose.yml | grep -A 5 volumes:
```

### 6. 복원 후 데이터가 보이지 않음

시퀀스 재설정:

```bash
docker exec -it mohe-postgres psql -U mohe_user -d mohe_db <<EOF
-- 시퀀스 재설정
SELECT setval('places_id_seq', (SELECT MAX(id) FROM places));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
-- 인덱스 재구성
REINDEX DATABASE mohe_db;
-- VACUUM 실행
VACUUM ANALYZE;
EOF
```

## 자동화 스크립트

### 정기 백업 자동화 (cron)

Mac에서 매일 자동 백업:

```bash
# crontab 편집
crontab -e

# 매일 새벽 2시에 백업
0 2 * * * cd /Users/andrewlim/Desktop/Developer/Mohe/MoheSpring && ./scripts/db-backup.sh >> /tmp/mohe-backup.log 2>&1

# 또는 7일 이상 된 백업 자동 삭제
0 3 * * * find /Users/andrewlim/Desktop/Developer/Mohe/MoheSpring/db-backups -name "*.sql*" -mtime +7 -delete
```

## 데이터베이스 정보 확인

```bash
# 데이터베이스 크기 확인
docker exec -it mohe-postgres psql -U mohe_user -d mohe_db -c \
  "SELECT pg_size_pretty(pg_database_size('mohe_db'));"

# 테이블별 크기
docker exec -it mohe-postgres psql -U mohe_user -d mohe_db -c \
  "SELECT tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
   FROM pg_tables WHERE schemaname = 'public' ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"

# 연결 정보
docker exec -it mohe-postgres psql -U mohe_user -d mohe_db -c \
  "SELECT * FROM pg_stat_activity WHERE datname = 'mohe_db';"
```

## Mac Mini 초기 설정

Mac Mini에서 처음 시작하는 경우:

```bash
# 1. 프로젝트 클론
git clone <repository-url> ~/Developer/Mohe/MoheSpring
cd ~/Developer/Mohe/MoheSpring

# 2. 환경 변수 설정
cp .env.example .env
# .env 파일 수정하여 실제 값 입력

# 3. Docker Compose로 서비스 시작 (데이터베이스만)
docker compose up -d postgres

# 4. 백업 파일 복원
./scripts/db-restore.sh ~/mohe_backup.sql

# 5. 전체 애플리케이션 시작
docker compose up -d

# 6. 확인
docker ps
curl http://localhost:8080/health
```

## 참고 사항

- **백업 주기**: 중요한 데이터는 최소 일 1회 백업 권장
- **보관 위치**: 백업 파일은 별도의 외장 하드나 클라우드에 보관
- **테스트**: 복원 테스트를 주기적으로 수행하여 백업 무결성 확인
- **암호화**: 민감한 데이터는 백업 파일을 암호화하여 보관
- **버전 관리**: 백업 파일명에 타임스탬프를 포함하여 버전 관리

## 관련 문서

- [README.md](./README.md) - 프로젝트 개요
- [CLAUDE.md](./CLAUDE.md) - 개발 가이드
- [docker-compose.yml](./docker-compose.yml) - Docker 설정
