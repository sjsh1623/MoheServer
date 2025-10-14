# Ollama 모델 전송 가이드

Ollama 모델(kanana-instruct 등)을 다른 컴퓨터로 쉽게 옮기는 방법을 설명합니다.

## 개요

Ollama 모델은 크기가 매우 큽니다 (수 GB). 인터넷에서 다시 다운로드하지 않고, 한 번 설정한 모델을 tar로 압축하여 다른 컴퓨터로 빠르게 옮길 수 있습니다.

## 방법 1: 스크립트 사용 (권장)

### 현재 컴퓨터에서 모델 추출

```bash
# kanana-instruct 모델 추출
./scripts/ollama-export.sh kanana-instruct

# 또는 다른 모델
./scripts/ollama-export.sh mxbai-embed-large
```

**출력 예시:**
```
================================================
   Ollama Model Export Script
================================================

📦 Exporting model: kanana-instruct

📋 Checking if model exists...
✅ Model found

📊 Model size: 4.7GB

📝 Creating Modelfile...
✅ Modelfile saved: ./ollama-models/kanana-instruct_Modelfile

💾 Extracting model blob...
📄 Reading manifest: /Users/andrew/.ollama/models/manifests/...
🔑 Blob SHA: sha256:abc123...
📦 Copying blob file...
✅ Blob saved: ./ollama-models/kanana-instruct.gguf

🗜️  Creating tar archive...
✅ Archive created: ./ollama-models/kanana-instruct_20250114_120000.tar.gz

================================================
📊 Export Summary
================================================
Model Name:    kanana-instruct
Original Size: 4.7GB
Archive Size:  4.2GB
Output File:   ./ollama-models/kanana-instruct_20250114_120000.tar.gz
================================================

📤 To transfer to another machine:
   scp ollama-models/kanana-instruct_20250114_120000.tar.gz user@mac-mini:~/

📥 To import on another machine:
   ./scripts/ollama-import.sh kanana-instruct_20250114_120000.tar.gz

✅ Export completed successfully!
```

### Mac Mini로 전송

**방법 A: SCP 사용**
```bash
scp ollama-models/kanana-instruct_*.tar.gz username@mac-mini-ip:~/
```

**방법 B: USB 드라이브**
```bash
# USB에 복사
cp ollama-models/kanana-instruct_*.tar.gz /Volumes/USB/

# Mac Mini에서 프로젝트로 복사
cp /Volumes/USB/kanana-instruct_*.tar.gz ~/Developer/Mohe/MoheSpring/
```

**방법 C: AirDrop (macOS)**
- Finder에서 `ollama-models` 폴더 열기
- tar.gz 파일을 Mac Mini로 AirDrop

### Mac Mini에서 모델 가져오기

```bash
# 프로젝트 디렉토리로 이동
cd ~/Developer/Mohe/MoheSpring

# 모델 가져오기
./scripts/ollama-import.sh ~/kanana-instruct_20250114_120000.tar.gz
```

**출력 예시:**
```
================================================
   Ollama Model Import Script
================================================

📂 Extracting archive to temporary directory...
✅ Archive extracted
   Model Name: kanana-instruct
   Modelfile:  kanana-instruct_Modelfile
   GGUF File:  kanana-instruct.gguf

📝 Preparing Modelfile...
🚀 Creating model in Ollama...
   This may take a few minutes...

✅ Model imported successfully!

🔍 Verifying installation...
✅ Model 'kanana-instruct' is available

kanana-instruct:latest    4.7 GB    5 minutes ago

💡 Test the model:
   ollama run kanana-instruct "안녕하세요"

================================================
✅ Import completed successfully!
================================================
```

### 테스트

```bash
# 모델 테스트
ollama run kanana-instruct "안녕하세요"

# 모델 목록 확인
ollama list
```

## 방법 2: 수동 방법

스크립트 없이 수동으로 처리하는 방법입니다.

### 1. Modelfile 저장

```bash
# Modelfile 추출
ollama show kanana-instruct --modelfile > kanana_Modelfile
```

### 2. 모델 블롭 찾기

```bash
# macOS 기준
OLLAMA_PATH="$HOME/.ollama/models"

# 모델 매니페스트 경로 찾기
find "$OLLAMA_PATH/manifests" -name "*kanana*"

# 최신 매니페스트 확인
cat "$OLLAMA_PATH/manifests/registry.ollama.ai/library/kanana-instruct/latest"
```

매니페스트 예시:
```json
{
  "schemaVersion": 2,
  "mediaType": "application/vnd.ollama.image.model",
  "config": {
    "digest": "sha256:abc123...",
    "size": 4700000000
  },
  "layers": [
    {
      "digest": "sha256:def456...",
      "size": 4700000000
    }
  ]
}
```

### 3. 블롭 파일 복사

```bash
# digest에서 SHA 추출 (예: sha256:def456...)
BLOB_SHA="def456..."

# 블롭 파일 복사
cp "$HOME/.ollama/models/blobs/sha256-$BLOB_SHA" kanana-instruct.gguf
```

### 4. 압축

```bash
# tar로 압축
tar -czf kanana-instruct.tar.gz kanana_Modelfile kanana-instruct.gguf

# 파일 크기 확인
ls -lh kanana-instruct.tar.gz
```

### 5. 다른 컴퓨터에서 가져오기

```bash
# 압축 해제
mkdir kanana-temp
tar -xzf kanana-instruct.tar.gz -C kanana-temp
cd kanana-temp

# Modelfile 수정 (FROM 경로 변경)
cat kanana_Modelfile
# FROM /path/to/model  <- 이 부분을

# 현재 경로로 변경
sed -i '' 's|FROM .*|FROM ./kanana-instruct.gguf|' kanana_Modelfile

# Ollama로 모델 생성
ollama create kanana-instruct -f kanana_Modelfile

# 확인
ollama list
```

## 방법 3: Docker Volume 사용 (Docker 환경)

Docker로 Ollama를 실행하는 경우:

### Volume 백업

```bash
# Ollama Docker Volume 찾기
docker volume ls | grep ollama

# Volume 백업
docker run --rm \
  -v ollama_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/ollama_volume.tar.gz -C /data .

# 파일 크기 확인
ls -lh ollama_volume.tar.gz
```

### Mac Mini로 전송 및 복원

```bash
# 1. Mac Mini로 전송
scp ollama_volume.tar.gz user@mac-mini:~/

# 2. Mac Mini에서 복원
cd ~/Developer/Mohe/MoheSpring

# Docker Compose로 Ollama 시작 (Volume 생성)
docker compose up -d ollama
docker compose stop ollama

# Volume에 데이터 복원
docker run --rm \
  -v mohespring_ollama_data:/data \
  -v $(pwd):/backup \
  alpine sh -c "cd /data && tar xzf /backup/ollama_volume.tar.gz"

# Ollama 재시작
docker compose up -d ollama

# 확인
docker exec -it mohe-ollama ollama list
```

## 문제 해결

### 1. "Model not found" 에러

현재 설치된 모델 확인:
```bash
ollama list
```

모델 이름을 정확히 입력했는지 확인:
```bash
# 잘못된 예
./scripts/ollama-export.sh kanana

# 올바른 예
./scripts/ollama-export.sh kanana-instruct
```

### 2. "Permission denied" 에러

스크립트 실행 권한 확인:
```bash
chmod +x scripts/ollama-export.sh
chmod +x scripts/ollama-import.sh
```

### 3. "Blob file not found" 에러

Ollama 모델 경로 확인:
```bash
# macOS
ls -la ~/.ollama/models/

# Linux
ls -la ~/.ollama/models/

# Docker
docker exec ollama ls -la /root/.ollama/models/
```

### 4. 용량 부족

대용량 모델(10GB+)의 경우 충분한 디스크 공간 확보:
```bash
# 사용 가능한 공간 확인
df -h .

# 불필요한 Ollama 모델 삭제
ollama list
ollama rm <unused-model>
```

### 5. 가져오기 후 모델이 느림

모델 재로드:
```bash
# 모델 언로드
ollama rm kanana-instruct

# 다시 가져오기
./scripts/ollama-import.sh kanana-instruct_*.tar.gz
```

## 여러 모델 한번에 전송

여러 모델을 한 번에 압축:

```bash
# 여러 모델 추출
./scripts/ollama-export.sh kanana-instruct
./scripts/ollama-export.sh mxbai-embed-large

# 모든 모델을 하나의 tar로 압축
cd ollama-models
tar -czf all-models.tar.gz *.tar.gz

# Mac Mini로 전송
scp all-models.tar.gz user@mac-mini:~/

# Mac Mini에서 압축 해제 및 가져오기
tar -xzf all-models.tar.gz
for model in *.tar.gz; do
    ~/Developer/Mohe/MoheSpring/scripts/ollama-import.sh "$model"
done
```

## 네트워크 전송 최적화

### 압축 레벨 조정

더 작은 파일 크기 (느린 압축):
```bash
# 스크립트 수정 필요
tar -czvf --best kanana-instruct.tar.gz ...
```

빠른 압축 (큰 파일 크기):
```bash
tar -czvf --fast kanana-instruct.tar.gz ...
```

### rsync 사용

네트워크 중단 시 재개 가능:
```bash
rsync -avz --progress \
  ollama-models/kanana-instruct_*.tar.gz \
  user@mac-mini:~/
```

## 모델 정보 확인

### 모델 크기 확인

```bash
# Ollama 명령어
ollama list

# 실제 파일 크기
du -sh ~/.ollama/models/
```

### 모델 상세 정보

```bash
# Modelfile 확인
ollama show kanana-instruct --modelfile

# 모델 파라미터
ollama show kanana-instruct
```

## 참고 사항

- **압축 시간**: 큰 모델(5GB+)은 압축에 5-10분 소요
- **네트워크 속도**: 1GB/min (기가비트 랜 기준)
- **스토리지**: 원본 + 압축본 공간 필요 (약 2배)
- **검증**: 가져오기 후 반드시 테스트 실행
- **백업**: 중요한 커스텀 모델은 별도 백업 권장

## 자동화 스크립트

모든 Ollama 모델 자동 백업:

```bash
#!/bin/bash
# backup-all-ollama-models.sh

BACKUP_DIR="./ollama-backups"
mkdir -p "$BACKUP_DIR"

# 모든 모델 추출
ollama list | tail -n +2 | awk '{print $1}' | while read model; do
    echo "Backing up: $model"
    ./scripts/ollama-export.sh "$model" "$BACKUP_DIR"
done

echo "All models backed up to: $BACKUP_DIR"
```

## 관련 문서

- [KANANA_AI_SETUP.md](./KANANA_AI_SETUP.md) - Kanana AI 초기 설정
- [DISTRIBUTED_CRAWLING.md](./DISTRIBUTED_CRAWLING.md) - 분산 크롤링 가이드
- [DATABASE_MIGRATION.md](./DATABASE_MIGRATION.md) - 데이터베이스 이전
