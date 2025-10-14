#!/bin/bash

# Ollama Model Import Script
# tar로 압축된 Ollama 모델을 현재 머신에 가져오기

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Docker 사용 여부 확인
DOCKER_CONTAINER=""
if docker ps --format '{{.Names}}' | grep -q "ollama"; then
    DOCKER_CONTAINER=$(docker ps --format '{{.Names}}' | grep "ollama" | head -1)
    echo -e "${YELLOW}🐳 Detected Ollama running in Docker: $DOCKER_CONTAINER${NC}"
    echo ""
fi

# 입력 파일 확인
TAR_FILE="$1"

if [ -z "$TAR_FILE" ]; then
    echo -e "${RED}Usage: $0 <model_tar_file>${NC}"
    echo ""
    echo "Example:"
    echo "  $0 ollama-models/kanana-instruct_20250114_120000.tar.gz"
    exit 1
fi

if [ ! -f "$TAR_FILE" ]; then
    echo -e "${RED}❌ Error: File not found: $TAR_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}   Ollama Model Import Script${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""

# 임시 디렉토리 생성
TEMP_DIR=$(mktemp -d)
echo -e "${YELLOW}📂 Extracting archive to temporary directory...${NC}"
tar -xzf "$TAR_FILE" -C "$TEMP_DIR"

# 파일 확인
MODELFILE=$(find "$TEMP_DIR" -name "*_Modelfile" | head -1)
GGUF_FILE=$(find "$TEMP_DIR" -name "*.gguf" | head -1)

if [ -z "$MODELFILE" ] || [ -z "$GGUF_FILE" ]; then
    echo -e "${RED}❌ Error: Invalid archive format${NC}"
    echo "Expected files: *_Modelfile and *.gguf"
    rm -rf "$TEMP_DIR"
    exit 1
fi

# 모델 이름 추출
MODEL_NAME=$(basename "$MODELFILE" | sed 's/_Modelfile$//')

echo -e "${GREEN}✅ Archive extracted${NC}"
echo -e "   Model Name: $MODEL_NAME"
echo -e "   Modelfile:  $(basename $MODELFILE)"
echo -e "   GGUF File:  $(basename $GGUF_FILE)"
echo ""

# Ollama 명령어 설정 (Docker 또는 로컬)
if [ -n "$DOCKER_CONTAINER" ]; then
    OLLAMA_CMD="docker exec $DOCKER_CONTAINER ollama"
else
    OLLAMA_CMD="ollama"
fi

# Ollama에 모델 이미 있는지 확인
if $OLLAMA_CMD list | grep -q "$MODEL_NAME"; then
    echo -e "${YELLOW}⚠️  Model '$MODEL_NAME' already exists${NC}"
    read -p "Do you want to overwrite? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}Import cancelled${NC}"
        rm -rf "$TEMP_DIR"
        exit 0
    fi
    echo -e "${YELLOW}🗑️  Removing existing model...${NC}"
    $OLLAMA_CMD rm "$MODEL_NAME" || true
fi

# Docker 또는 로컬에서 모델 생성
if [ -n "$DOCKER_CONTAINER" ]; then
    # Docker: 파일을 컨테이너로 복사 후 생성
    echo -e "${YELLOW}📦 Copying files to Docker container...${NC}"

    # GGUF 파일을 컨테이너로 복사
    docker cp "$GGUF_FILE" "${DOCKER_CONTAINER}:/tmp/${MODEL_NAME}.gguf"

    # Modelfile 수정 (Docker 내부 경로 사용)
    UPDATED_MODELFILE="$TEMP_DIR/Modelfile"
    sed "s|FROM .*|FROM /tmp/${MODEL_NAME}.gguf|" "$MODELFILE" > "$UPDATED_MODELFILE"

    # Modelfile을 컨테이너로 복사
    docker cp "$UPDATED_MODELFILE" "${DOCKER_CONTAINER}:/tmp/Modelfile"

    # 컨테이너 내에서 모델 생성
    echo -e "${YELLOW}🚀 Creating model in Ollama (Docker)...${NC}"
    echo -e "${YELLOW}   This may take a few minutes...${NC}"
    echo ""

    docker exec $DOCKER_CONTAINER ollama create "$MODEL_NAME" -f /tmp/Modelfile

    # 임시 파일 정리
    docker exec $DOCKER_CONTAINER rm -f /tmp/${MODEL_NAME}.gguf /tmp/Modelfile
else
    # 로컬: 직접 생성
    echo -e "${YELLOW}📝 Preparing Modelfile...${NC}"
    UPDATED_MODELFILE="$TEMP_DIR/Modelfile"
    sed "s|FROM .*|FROM $GGUF_FILE|" "$MODELFILE" > "$UPDATED_MODELFILE"

    # Ollama로 모델 생성
    echo -e "${YELLOW}🚀 Creating model in Ollama...${NC}"
    echo -e "${YELLOW}   This may take a few minutes...${NC}"
    echo ""

    ollama create "$MODEL_NAME" -f "$UPDATED_MODELFILE"
fi

echo ""
echo -e "${GREEN}✅ Model imported successfully!${NC}"
echo ""

# 정리
rm -rf "$TEMP_DIR"

# 검증
echo -e "${YELLOW}🔍 Verifying installation...${NC}"
if $OLLAMA_CMD list | grep -q "$MODEL_NAME"; then
    echo -e "${GREEN}✅ Model '$MODEL_NAME' is available${NC}"
    echo ""
    $OLLAMA_CMD list | grep "$MODEL_NAME"
    echo ""

    echo -e "${YELLOW}💡 Test the model:${NC}"
    if [ -n "$DOCKER_CONTAINER" ]; then
        echo -e "   docker exec $DOCKER_CONTAINER ollama run $MODEL_NAME \"안녕하세요\""
    else
        echo -e "   ollama run $MODEL_NAME \"안녕하세요\""
    fi
    echo ""
else
    echo -e "${RED}❌ Verification failed${NC}"
    exit 1
fi

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}✅ Import completed successfully!${NC}"
echo -e "${GREEN}================================================${NC}"
