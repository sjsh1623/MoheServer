# Mohe 데이터 파이프라인

## 전체 아키텍처

```
[1] Kakao 키워드 수집 (MoheCrawler :4001)
│   키워드("카페","맛집"...) + 좌표 + radius=1km
│   시군구를 1km 격자로 분할하여 전국 커버
│   → places (crawl_status=PENDING)
↓
[2] 네이버 크롤링 + AI 설명 (MoheSpring, 30분마다)
│   Selenium으로 리뷰/설명/이미지/영업시간/SNS 수집
│   GPT-5-mini로 설명(160-230자) + 키워드 9개 생성
│   → crawl_status=COMPLETED, embed_status=PENDING
↓
[3] 키워드 임베딩 (MoheSpring, 45분마다)
│   keyword_embeddings 룩업 캐시로 중복 API 스킵
│   신규 키워드만 OpenAI text-embedding-3-small (1536D)
│   → embed_status=COMPLETED
↓
[4] 이미지 후처리 (MoheSpring, 60분마다)
│   → 서비스 노출 가능 상태
```

## API 키 & 모델 설정

| 서비스 | 환경변수 | 값 | 용도 |
|--------|---------|-----|------|
| OpenAI | `OPENAI_API_KEY` | .env | 설명 생성 + 임베딩 |
| OpenAI | `OPENAI_MODEL` | gpt-5-mini | 장소 설명/키워드 생성 |
| OpenAI | `EMBEDDING_MODEL` | text-embedding-3-small | 키워드 벡터화 (1536D) |
| Kakao | `KAKAO_REST_API_KEY` | .env | 장소 수집 API |
| Kakao | `KAKAO_API_KEY` | .env (MoheSpring) | 장소 검색 |

## 컨테이너 & 네트워크

| 컨테이너 | 포트 | 역할 |
|---------|------|------|
| `spring` | 8080 (내부), 8000 (외부) | API 서버 + 배치 스케줄러 |
| `mohe-batch-crawler` | 2000 | 네이버 Selenium 크롤러 |
| `mohe-batch-crawler` | 4001 | Kakao 장소 수집 API |
| `mohe-postgres` | 5432 (내부), 16239 (외부) | PostgreSQL + pgvector |
| `mohe-batch` | 8081 | 분산 배치 서버 (선택) |

## Kakao 수집 방식

### 키워드 + 좌표 기반 (/keyword.json)
```
키워드: 카페, 맛집, 레스토랑, 데이트, 바, 공방, 취미생활, 쇼핑

일반 수집: 시군구를 1km 격자로 분할
→ 각 격자 포인트: 8키워드 × 좌표 + radius=1000m
→ 시군구당 ~29개 격자 × 8키워드 = ~232 API 세션

사용자 위치: 키워드 + 사용자 좌표 + radius=2000m
→ priority=1 (최우선)
```

### 일일 API 한도
- Kakao: 100,000 호출/일
- Tour API: 10,000 호출/일

### 자동 순환
- 완료 지역 30일 후 자동 재수집 (priority=3)
- 2시간마다 수집 상태 체크 → 멈추면 자동 재시작

## 임베딩 시스템

### 룩업 캐시 (keyword_embeddings 테이블)
```
동일 키워드는 한 번만 OpenAI API 호출
→ keyword_embeddings에 글로벌 캐시 저장
→ 이후 같은 키워드는 DB에서 조회 (API 호출 0)
→ 장소별 place_keyword_embeddings에 복사

예: "카페"가 1만 개 장소에 있어도 API 호출은 1번
```

### 차원 & 모델
- 모델: text-embedding-3-small
- 차원: 1536D
- DB 타입: pgvector vector(1536)

## 스케줄러 (BatchSchedulerService)

| Job | 간격 | 초기 딜레이 |
|-----|------|------------|
| Kakao 큐 수집 체크 | 2시간 | 5분 |
| UpdateCrawledDataJob | 30분 | 60초 |
| VectorEmbeddingJob | 45분 | 120초 |
| ImageUpdateJob | 60분 | 180초 |

## 크롤링 시 수집되는 데이터

크롤링 성공한 장소에는 다음이 모두 저장됨:

| 테이블 | 내용 | 수량 |
|--------|------|------|
| `place_descriptions` | AI 생성 설명 (mohe_description) + 원본 설명 | 1개/장소 |
| `place_reviews` | 네이버 리뷰 | 최대 10개/장소 |
| `place_images` | 이미지 URL | 최대 5개/장소 |
| `place_business_hours` | 요일별 영업시간 | 7개/장소 |
| `place_sns` | 인스타/블로그 등 | 0~N개/장소 |
| `place_menus` | 메뉴명/가격 | 0~N개/장소 |

> **주의**: `place_reviews`, `place_images`, `place_descriptions`의 `created_at`은 원본 데이터 시점.
> 최신 데이터 확인 시 `places.updated_at` 기준으로 JOIN해야 함.

## 모니터링 명령어

```bash
# DB 상태
docker exec mohe-postgres psql -U mohe_user -d mohe_db -c \
  "SELECT crawl_status, embed_status, COUNT(*) FROM places GROUP BY crawl_status, embed_status;"

# 임베딩 캐시
docker exec mohe-postgres psql -U mohe_user -d mohe_db -c \
  "SELECT COUNT(*) FROM keyword_embeddings;"

# 장소별 임베딩
docker exec mohe-postgres psql -U mohe_user -d mohe_db -c \
  "SELECT COUNT(DISTINCT place_id) FROM place_keyword_embeddings;"

# 크롤링 로그
docker logs -f spring 2>&1 | grep -E "📖|✅|❌|🧮"

# Kakao 수집 상태
curl -s http://localhost:4001/api/batch/queue-status | python3 -m json.tool
```

## 비용

| 작업 | 모델 | 비용 |
|------|------|------|
| 설명 생성 | gpt-5-mini | ~$0.001/장소 |
| 임베딩 (신규 키워드) | text-embedding-3-small | ~$0.0002/키워드 |
| 임베딩 (캐시 히트) | - | 무료 |
| 평시 월간 | 전체 | ~$121/월 |
