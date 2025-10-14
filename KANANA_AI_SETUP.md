# Kanana AI Setup Guide

이 문서는 Mohe 프로젝트에서 Hugging Face의 Kanana AI 모델을 Ollama에 설치하고 사용하는 방법을 설명합니다.

## 목차
1. [Kanana AI 소개](#kanana-ai-소개)
2. [모델 다운로드](#모델-다운로드)
3. [Modelfile 작성](#modelfile-작성)
4. [Ollama에 모델 등록](#ollama에-모델-등록)
5. [환경 변수 설정](#환경-변수-설정)
6. [테스트](#테스트)

## Kanana AI 소개

**Kanana-instruct**는 한국어에 최적화된 대형 언어 모델(LLM)입니다.
- **출처**: Hugging Face
- **특징**: 한국어 자연어 처리, 지시사항 수행, 대화형 AI
- **용도**: Mohe 프로젝트에서 장소 설명, MBTI 분석, 리뷰 요약 등에 사용

## 모델 다운로드

### 1. Hugging Face에서 모델 다운로드

```bash
# Hugging Face CLI 설치 (필요한 경우)
pip install huggingface-hub

# 모델 다운로드 (예시 - 실제 모델명으로 변경)
huggingface-cli download <model-repo-id> --local-dir ./kanana-model

# 또는 Git LFS를 사용하여 직접 클론
git lfs install
git clone https://huggingface.co/<model-repo-id> ./kanana-model
```

### 2. 모델 파일 확인

다운로드 후 다음 파일들이 있는지 확인:
```
kanana-model/
├── config.json
├── tokenizer.json
├── tokenizer_config.json
├── model.safetensors (또는 .bin 파일)
└── special_tokens_map.json
```

## Modelfile 작성

Ollama에 모델을 등록하기 위한 Modelfile을 작성합니다.

### Modelfile 예시

프로젝트 루트에 `Modelfile.kanana`를 생성:

```dockerfile
# Kanana AI Modelfile for Ollama

# 베이스 모델 또는 모델 파일 경로 지정
FROM ./kanana-model/model.safetensors

# 모델 파라미터 설정
PARAMETER temperature 0.7
PARAMETER top_p 0.9
PARAMETER top_k 40
PARAMETER num_ctx 4096
PARAMETER num_predict 2048

# 시스템 프롬프트 (한국어 응답 유도)
TEMPLATE """{{ if .System }}<|system|>
{{ .System }}</s>
{{ end }}{{ if .Prompt }}<|user|>
{{ .Prompt }}</s>
{{ end }}<|assistant|>
{{ .Response }}</s>
"""

# 시스템 메시지
SYSTEM """당신은 한국어로 대화하는 친절한 AI 어시스턴트입니다.
사용자의 질문에 정확하고 자세하게 답변하세요."""

# 라이센스 정보
LICENSE """MIT License"""

# 메시지 템플릿 (모델에 따라 조정 필요)
MESSAGE user 안녕하세요
MESSAGE assistant 안녕하세요! 무엇을 도와드릴까요?
```

### Modelfile 파라미터 설명

- **FROM**: 모델 파일 경로 또는 베이스 모델
- **PARAMETER temperature**: 응답의 창의성 (0.0-1.0, 낮을수록 결정적)
- **PARAMETER top_p**: Nucleus sampling threshold
- **PARAMETER top_k**: Top-K sampling 값
- **PARAMETER num_ctx**: 컨텍스트 윈도우 크기 (토큰 수)
- **PARAMETER num_predict**: 최대 생성 토큰 수
- **TEMPLATE**: 프롬프트 템플릿 (모델 아키텍처에 따라 다름)
- **SYSTEM**: 시스템 프롬프트

## Ollama에 모델 등록

### 1. Ollama 설치 확인

```bash
# Ollama가 설치되어 있는지 확인
ollama --version

# Ollama 서비스 시작 (macOS/Linux)
ollama serve
```

### 2. 모델 생성 및 등록

```bash
# Modelfile을 사용하여 모델 생성
ollama create kanana-instruct -f Modelfile.kanana

# 모델 생성 확인
ollama list
```

출력 예시:
```
NAME                    ID              SIZE    MODIFIED
kanana-instruct         abc123...       7.4 GB  2 minutes ago
```

### 3. 모델 테스트

```bash
# 터미널에서 직접 테스트
ollama run kanana-instruct "안녕하세요, 서울의 유명한 관광지를 추천해주세요."

# 또는 대화형 모드
ollama run kanana-instruct
```

## 환경 변수 설정

### 1. .env 파일 생성

`.env.example`을 복사하여 `.env` 파일 생성:

```bash
cp .env.example .env
```

### 2. Kanana AI 관련 환경 변수 설정

`.env` 파일을 열어 다음 값을 설정:

```bash
# ===========================================
# Ollama Configuration (AI 모델)
# ===========================================
# Ollama 서버 URL
OLLAMA_HOST=http://ollama:11434              # Docker 환경
OLLAMA_BASE_URL=http://localhost:11434       # 로컬 개발 환경

# Kanana AI 모델 설정
LLM_OLLAMA_MODEL=kanana-instruct
OLLAMA_TEXT_MODEL=kanana-instruct

# 임베딩 모델 (추천 시스템용)
OLLAMA_EMBEDDING_MODEL=mxbai-embed-large:latest

# Ollama API 타임아웃 (초)
OLLAMA_TIMEOUT=120

# OpenAI 사용 여부 (Kanana AI 사용 시 false)
LLM_OPENAI_ACTIVE=false
LLM_OPENAI_API_KEY=
LLM_OPENAI_MODEL=gpt-3.5-turbo
```

### 3. 기타 필수 환경 변수

```bash
# JWT 시크릿 (64자 이상 권장)
JWT_SECRET=your-super-secure-jwt-secret-key-minimum-64-characters-long

# Kakao API (장소 검색)
KAKAO_API_KEY=your_kakao_rest_api_key

# Naver API (장소 검색)
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret

# Database
DB_USERNAME=mohe_user
DB_PASSWORD=mohe_password
```

## 테스트

### 1. Docker Compose로 전체 스택 실행

```bash
# Docker Compose로 모든 서비스 시작
docker compose up --build

# 또는 백그라운드 실행
docker compose up -d
```

### 2. Ollama 컨테이너에서 모델 확인

```bash
# Ollama 컨테이너 접속
docker exec -it mohe-ollama bash

# 모델 목록 확인
ollama list

# 모델이 없다면 생성 (호스트에서 Modelfile 복사 필요)
ollama create kanana-instruct -f /path/to/Modelfile.kanana
```

### 3. Spring Boot 애플리케이션에서 테스트

```bash
# 애플리케이션 로그 확인
docker logs -f mohe-spring-app

# API 테스트 (장소 설명 생성)
curl -X POST http://localhost:8080/api/places/generate-description \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "placeId": 1,
    "prompt": "이 장소에 대한 매력적인 설명을 작성해주세요."
  }'
```

### 4. Ollama API 직접 호출 테스트

```bash
# Ollama API 테스트
curl http://localhost:11434/api/generate -d '{
  "model": "kanana-instruct",
  "prompt": "한국의 전통 음식에 대해 설명해주세요.",
  "stream": false
}'
```

## 문제 해결

### 모델이 로드되지 않는 경우

1. Ollama 서비스 상태 확인:
   ```bash
   docker ps | grep ollama
   docker logs mohe-ollama
   ```

2. 모델 파일 경로 확인:
   ```bash
   # Ollama 컨테이너에서 모델 디렉토리 확인
   docker exec mohe-ollama ls -la /root/.ollama/models
   ```

3. Modelfile 문법 확인:
   - `FROM` 경로가 정확한지 확인
   - 모델 파일 형식이 지원되는지 확인 (GGUF, SafeTensors 등)

### 메모리 부족 에러

Kanana AI 모델이 큰 경우 Docker에 충분한 메모리 할당:

```bash
# Docker Desktop 설정에서 메모리 증가 (최소 8GB 권장)
# 또는 docker-compose.yml에 리소스 제한 추가
```

### 응답 속도가 느린 경우

1. **GPU 사용 설정** (NVIDIA GPU가 있는 경우):
   ```yaml
   # docker-compose.yml
   ollama:
     image: ollama/ollama:latest
     runtime: nvidia
     environment:
       - NVIDIA_VISIBLE_DEVICES=all
   ```

2. **컨텍스트 윈도우 축소**:
   Modelfile에서 `num_ctx` 값을 줄임 (예: 4096 → 2048)

3. **양자화 모델 사용**:
   더 작은 양자화 버전의 모델 사용 (Q4, Q5 등)

## 로컬 개발 환경

### macOS/Linux에서 Ollama 직접 실행

```bash
# Ollama 설치
curl -fsSL https://ollama.com/install.sh | sh

# 또는 Homebrew (macOS)
brew install ollama

# Ollama 서비스 시작
ollama serve

# 별도 터미널에서 모델 생성
ollama create kanana-instruct -f Modelfile.kanana

# 애플리케이션 실행 (환경 변수 로드)
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 참고 자료

- [Ollama 공식 문서](https://github.com/ollama/ollama)
- [Modelfile 문법](https://github.com/ollama/ollama/blob/main/docs/modelfile.md)
- [Hugging Face 모델 다운로드](https://huggingface.co/docs/hub/models-downloading)
- [Mohe 프로젝트 문서](./README.md)

## 라이센스

이 프로젝트는 MIT 라이센스를 따릅니다. Kanana AI 모델의 라이센스는 Hugging Face 저장소를 참조하세요.
