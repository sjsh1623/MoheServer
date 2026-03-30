# Mohe 데이터 파이프라인

## 전체 아키텍처

```
[1] Kakao 키워드 수집 (MoheCrawler :4001)
│   키워드("카페","맛집"...) + 좌표 + radius=1km
│   시군구를 1km 격자로 분할하여 전국 커버
│   → places (crawl_status=PENDING)
↓
[2] 네이버 크롤링 + AI 설명 (MoheSpring, 30분마다)
│   Selenium으로 리뷰/설명/이미지/영업시간/SNS/메뉴 수집
│   GPT-4.1-mini로 설명(160-230자) + 키워드 9개 생성
│   사장님 대댓글 자동 필터링
│   → crawl_status=COMPLETED, embed_status=PENDING
↓
[3] 키워드 + 문장 임베딩 (MoheSpring, 45분마다)
│   1번 OpenAI API 호출로 키워드 9개 + 문장 1개 동시 임베딩
│   text-embedding-3-small (1536D)
│   keyword_embeddings 룩업 캐시 (94.5% 중복 절감)
│   → place_keyword_embeddings (카테고리 필터용)
│   → place_description_embeddings (프롬프트 검색용)
│   → embed_status=COMPLETED
↓
[4] 이미지 후처리 (MoheSpring, 60분마다)
│   → 서비스 노출 가능 상태
```

## API 키 & 모델 설정

| 서비스 | 환경변수 | 값 | 용도 |
|--------|---------|-----|------|
| OpenAI | `OPENAI_API_KEY` | .env | 설명 생성 + 임베딩 |
| OpenAI | `OPENAI_MODEL` | gpt-4.1-mini | 장소 설명/키워드 생성 |
| OpenAI | `EMBEDDING_MODEL` | text-embedding-3-small | 벡터화 (1536D) |
| Kakao | `KAKAO_REST_API_KEY` | .env | 장소 수집 API |

## 임베딩 구조

| 테이블 | 용도 | 장소당 | 검색 방식 |
|--------|------|--------|----------|
| `place_keyword_embeddings` | 카테고리 필터 | 9벡터 | SQL unnest 정확 매칭 |
| `place_description_embeddings` | 프롬프트 검색 | 1벡터 | cosine 유사도 (pgvector HNSW) |
| `keyword_embeddings` | 키워드 캐시 | - | 중복 API 호출 방지 |

## 검색 시스템

### 카테고리 기반 검색
```
GET /api/categories/{key}/places?lat=&lon=&limit=
→ SQL: category 배열 정확 매칭 + 거리 필터 (GIN 인덱스)
```

### 프롬프트 기반 검색
```
GET /api/search/prompt?query=비 오는 날 카페&latitude=&longitude=
→ 사용자 문장 임베딩 ↔ place_description_embeddings cosine 유사도
```

### 통합 검색 (하이브리드)
```
GET /api/search?q=파스타&lat=&lon=
→ 문장 벡터 검색 > 키워드 벡터 검색 > LIKE 키워드 merge
```

### 대화형 검색
```
POST /api/search/chat
→ 검색 + 대화 DB 저장 (search_conversations + search_messages)
→ 로그인: user_id 기반, 비로그인: sessionId(UUID) 기반

GET /api/search/chat/conversations — 대화 목록
GET /api/search/chat/conversations/{id} — 대화 상세
DELETE /api/search/chat/conversations/{id} — 대화 삭제
```

## 배치 잡

| Job | 간격 | 설명 |
|-----|------|------|
| `updateCrawledDataJob` | 30분 | 크롤링 + AI 설명 생성 |
| `vectorEmbeddingJob` | 45분 | 키워드 + 문장 임베딩 |
| `imageUpdateJob` | 60분 | 이미지 후처리 |
| `descriptionOnlyJob` | 수동 | 크롤링 스킵, 기존 데이터로 AI 설명 생성 |

### 좀비 잡 방지 (자동)
- 앱 시작: STARTED 잡 전부 FAILED 마킹
- 스케줄러: 1시간 이상 STARTED → 자동 FAILED + 재실행
- 앱 종료: 실행 중 잡 FAILED 마킹

## 크롤링 시 수집되는 데이터

| 테이블 | 내용 | 수량 |
|--------|------|------|
| `place_descriptions` | AI 설명 + 원본 설명 | 1개/장소 |
| `place_reviews` | 네이버 리뷰 (사장님 답글 제외) | 최대 10개/장소 |
| `place_images` | 이미지 URL | 최대 5개/장소 |
| `place_business_hours` | 요일별 영업시간 | 7개/장소 |
| `place_sns` | 인스타/블로그 등 | 0~N개/장소 |
| `place_menus` | 메뉴명/가격 | 0~N개/장소 |

## 홈 카테고리 추천

```
GET /api/categories/home?lat=&lon=&mbti=
→ 첫줄: MBTI 기반 "오늘은 이런 곳 어때요?" (동일 MBTI 좋아요 순)
→ 나머지: 시간+날씨 기반 30개 카테고리 (20개 규칙)
```

## 어드민 대시보드

```
GET /api/admin/monitor/pipeline/stats — 파이프라인 통계 (장소/임베딩/컨텐츠/잡 상태)
GET /api/admin/monitor/pipeline/recent-crawls — 최근 크롤링 장소 (위치)
GET /api/admin/monitor/pipeline/recent-activity — 24시간 수집량 차트
POST /api/admin/monitor/pipeline/jobs/{name}/trigger — 수동 Job 실행
```

## Kakao 수집 방식

```
키워드: 카페, 맛집, 레스토랑, 데이트, 바, 공방, 취미생활, 쇼핑
API: /keyword.json + 좌표(x,y) + radius=1000m
시군구별 1km 격자: generate_grid_points(center, 3km, 1km)
일일 한도: 100,000 호출
30일 후 완료 지역 자동 재수집 (priority=3)
```

## 모니터링 명령어

```bash
# DB 상태
docker exec mohe-postgres psql -U mohe_user -d mohe_db -c \
  "SELECT crawl_status, embed_status, COUNT(*) FROM places GROUP BY crawl_status, embed_status;"

# 임베딩 현황
docker exec mohe-postgres psql -U mohe_user -d mohe_db -c \
  "SELECT COUNT(*) FROM keyword_embeddings;
   SELECT COUNT(*) FROM place_description_embeddings;
   SELECT COUNT(DISTINCT place_id) FROM place_keyword_embeddings;"

# 크롤링 로그
docker logs -f spring 2>&1 | grep -E "📖|✅|❌|🧮"

# Kakao 수집 상태
curl -s http://localhost:4001/api/batch/queue-status | python3 -m json.tool

# 파이프라인 전체 통계
curl -s http://localhost:8000/api/admin/monitor/pipeline/stats | python3 -m json.tool
```

## 비용

| 작업 | 모델 | 비용 |
|------|------|------|
| 설명 생성 | gpt-4.1-mini | ~$0.001/장소 |
| 임베딩 (키워드+문장) | text-embedding-3-small | ~$0.0002/장소 (캐시 히트 시 무료) |
| 평시 월간 | 전체 | ~$121/월 |
