#!/bin/bash

# Ollama Model Export Script
# 현재 머신에서 Ollama 모델을 추출하여 다른 머신으로 이동 가능한 파일 생성

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 기본 설정
MODEL_NAME="${1:-kanana-instruct}"
OUTPUT_DIR="${2:-./ollama-models}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Docker 사용 여부 확인
DOCKER_CONTAINER=""
if docker ps --format '{{.Names}}' | grep -q "ollama"; then
    DOCKER_CONTAINER=$(docker ps --format '{{.Names}}' | grep "ollama" | head -1)
    echo -e "${YELLOW}🐳 Detected Ollama running in Docker: $DOCKER_CONTAINER${NC}"
fi

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}   Ollama Model Export Script${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""

# 출력 디렉토리 생성
mkdir -p "$OUTPUT_DIR"

echo -e "${YELLOW}📦 Exporting model: $MODEL_NAME${NC}"
echo ""

# Ollama 명령어 설정 (Docker 또는 로컬)
if [ -n "$DOCKER_CONTAINER" ]; then
    OLLAMA_CMD="docker exec $DOCKER_CONTAINER ollama"
else
    OLLAMA_CMD="ollama"
fi

# Ollama 모델 목록 확인
echo -e "${YELLOW}📋 Checking if model exists...${NC}"
if ! $OLLAMA_CMD list | grep -q "$MODEL_NAME"; then
    echo -e "${RED}❌ Error: Model '$MODEL_NAME' not found${NC}"
    echo ""
    echo "Available models:"
    $OLLAMA_CMD list
    exit 1
fi

echo -e "${GREEN}✅ Model found${NC}"
echo ""

# 모델 크기 확인
MODEL_SIZE=$($OLLAMA_CMD list | grep "$MODEL_NAME" | awk '{print $3}')
echo -e "${YELLOW}📊 Model size: $MODEL_SIZE${NC}"
echo ""

# Modelfile 생성
echo -e "${YELLOW}📝 Creating Modelfile...${NC}"
MODELFILE_PATH="$OUTPUT_DIR/${MODEL_NAME}_Modelfile"
$OLLAMA_CMD show $MODEL_NAME --modelfile > "$MODELFILE_PATH"
echo -e "${GREEN}✅ Modelfile saved: $MODELFILE_PATH${NC}"
echo ""

# 모델을 GGUF 파일로 추출
echo -e "${YELLOW}💾 Extracting model blob...${NC}"
BLOB_PATH="$OUTPUT_DIR/${MODEL_NAME}.gguf"

# Ollama 모델 저장 경로 찾기
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    OLLAMA_MODELS_PATH="$HOME/.ollama/models"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    OLLAMA_MODELS_PATH="$HOME/.ollama/models"
else
    echo -e "${RED}❌ Unsupported OS: $OSTYPE${NC}"
    exit 1
fi

# Modelfile에서 blob SHA 추출
echo -e "${YELLOW}📄 Extracting blob SHA from model...${NC}"
BLOB_SHA=$($OLLAMA_CMD show $MODEL_NAME --modelfile | grep "^FROM" | grep -o "sha256-[a-f0-9]*" | sed 's/sha256-//')

if [ -z "$BLOB_SHA" ]; then
    echo -e "${RED}❌ Error: Could not extract blob SHA from model${NC}"
    exit 1
fi

echo -e "${YELLOW}🔑 Blob SHA: sha256:$BLOB_SHA${NC}"

# Docker 또는 로컬에서 Blob 파일 복사
if [ -n "$DOCKER_CONTAINER" ]; then
    # Docker 컨테이너에서 파일 복사
    BLOB_FILE="/root/.ollama/models/blobs/sha256-$BLOB_SHA"

    echo -e "${YELLOW}📦 Copying blob from Docker container...${NC}"
    docker exec $DOCKER_CONTAINER test -f "$BLOB_FILE" || {
        echo -e "${RED}❌ Error: Blob file not found in container: $BLOB_FILE${NC}"
        exit 1
    }

    docker cp "${DOCKER_CONTAINER}:${BLOB_FILE}" "$BLOB_PATH"
    echo -e "${GREEN}✅ Blob saved: $BLOB_PATH${NC}"
else
    # 로컬 파일 복사
    BLOB_FILE="$OLLAMA_MODELS_PATH/blobs/sha256-$BLOB_SHA"

    if [ ! -f "$BLOB_FILE" ]; then
        echo -e "${RED}❌ Error: Blob file not found: $BLOB_FILE${NC}"
        exit 1
    fi

    echo -e "${YELLOW}📦 Copying blob file...${NC}"
    cp "$BLOB_FILE" "$BLOB_PATH"
    echo -e "${GREEN}✅ Blob saved: $BLOB_PATH${NC}"
fi

echo ""

# tar로 압축
TAR_FILE="$OUTPUT_DIR/${MODEL_NAME}_${TIMESTAMP}.tar.gz"
echo -e "${YELLOW}🗜️  Creating tar archive...${NC}"
tar -czf "$TAR_FILE" -C "$OUTPUT_DIR" "${MODEL_NAME}_Modelfile" "${MODEL_NAME}.gguf"

# 임시 파일 삭제
rm "$MODELFILE_PATH" "$BLOB_PATH"

echo -e "${GREEN}✅ Archive created: $TAR_FILE${NC}"
echo ""

# 파일 크기 확인
TAR_SIZE=$(du -h "$TAR_FILE" | awk '{print $1}')
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}📊 Export Summary${NC}"
echo -e "${GREEN}================================================${NC}"
echo -e "Model Name:    $MODEL_NAME"
echo -e "Original Size: $MODEL_SIZE"
echo -e "Archive Size:  $TAR_SIZE"
echo -e "Output File:   $TAR_FILE"
echo -e "${GREEN}================================================${NC}"
echo ""

echo -e "${YELLOW}📤 To transfer to another machine:${NC}"
echo -e "   scp $TAR_FILE user@mac-mini:~/"
echo ""
echo -e "${YELLOW}📥 To import on another machine:${NC}"
echo -e "   ./scripts/ollama-import.sh $TAR_FILE"
echo ""

echo -e "${GREEN}✅ Export completed successfully!${NC}"
