# MoheSpring

> 한국의 숨은 장소를 발견하고 MBTI 기반 개인화 추천을 제공하는 Spring Boot API 서버

## 기술 스택

- **Framework**: Spring Boot 3.2.0, Java 21
- **Database**: PostgreSQL + pgvector, Flyway 마이그레이션
- **Security**: Spring Security + JWT (Access/Refresh Token)
- **Batch**: Spring Batch 5.x (비동기 병렬 처리)
- **Build**: Gradle 8.5+ (Kotlin DSL)

## 시작하기

```bash
# Docker로 실행
docker compose --profile production up --build app

# 개발 모드 (Hot Reload)
docker compose --profile dev up --build app-dev

# 로컬 실행 (DB만 Docker)
docker compose up postgres -d && ./gradlew bootRun
```

### 환경 변수 (.env)

```bash
DB_USERNAME=mohe_user
DB_PASSWORD=your_password
JWT_SECRET=your_secret_key_minimum_64_characters
NAVER_CLIENT_ID=your_client_id
NAVER_CLIENT_SECRET=your_client_secret
EMBEDDING_SERVICE_URL=http://localhost:6000
OPENAI_API_KEY=your_openai_key           # 선택
VWORLD_API_KEY=your_vworld_key           # 역지오코딩
TOUR_API_KEY=your_tour_api_key           # 한국관광공사
KAKAO_API_KEY=your_kakao_key             # 카카오 로컬
GOVT_API_KEY=your_govt_key               # 행정구역
KMA_SERVICE_KEY=your_kma_key             # 기상청
```

## API 엔드포인트

### 인증 (`/api/auth`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/auth/login` | 이메일/비밀번호 로그인, JWT 발급 | - |
| POST | `/api/auth/signup` | 회원가입 시작, OTP 이메일 전송 | - |
| POST | `/api/auth/verify-email` | OTP 코드 인증 | - |
| POST | `/api/auth/check-nickname` | 닉네임 중복 확인 | - |
| POST | `/api/auth/setup-password` | 비밀번호 설정 및 가입 완료 | - |
| POST | `/api/auth/refresh` | Access Token 갱신 | - |
| POST | `/api/auth/logout` | 로그아웃 (Refresh Token 무효화) | - |
| POST | `/api/auth/forgot-password` | 비밀번호 재설정 이메일 요청 | - |
| POST | `/api/auth/reset-password` | 비밀번호 재설정 완료 | - |
| POST | `/api/auth/social/{provider}` | 소셜 로그인 (kakao/google) | - |
| GET | `/api/auth/social/linked` | 연결된 소셜 계정 조회 | USER |
| POST | `/api/auth/social/{provider}/unlink` | 소셜 계정 연결 해제 | USER |

### 사용자 (`/api/user`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/user/profile` | 프로필 조회 | USER |
| PUT | `/api/user/profile` | 닉네임/프로필 이미지 수정 | USER |
| POST | `/api/user/profile/image` | 프로필 이미지 업로드 (5MB) | USER |
| PUT | `/api/user/preferences` | MBTI/나이/공간/교통 수정 | USER |
| POST | `/api/user/agreements` | 약관 동의 저장 | USER |
| POST | `/api/user/onboarding/complete` | 온보딩 완료 처리 | USER |
| GET | `/api/user/recent-places` | 최근 본 장소 | USER |
| POST | `/api/user/recent-places` | 최근 본 장소 기록 | USER |
| GET | `/api/user/my-places` | 내 등록 장소 | USER |

### 장소 (`/api/places`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/places` | 장소 목록 (필터/정렬) | - |
| GET | `/api/places/{id}` | 장소 상세 | - |
| GET | `/api/places/{id}/menus` | 메뉴 목록 | - |
| GET | `/api/places/search` | 장소 검색 (날씨/시간/위치 컨텍스트) | - |
| GET | `/api/places/nearby` | 반경 내 주변 장소 (Haversine) | - |
| GET | `/api/places/popular` | 인기 장소 (리뷰수/평점순) | - |
| GET | `/api/places/list` | 일반 목록 (페이지네이션) | - |
| GET | `/api/places/recommendations` | 위치 가중 추천 (15km 70% + 30km 30%) | - |
| GET | `/api/places/new` | 신규 추천 | - |
| GET | `/api/places/current-time` | 시간대 기반 추천 | - |
| GET | `/api/places/vector-search` | 벡터 유사도 검색 | USER |
| GET | `/api/places/debug` | 디버그 통계 | - |

### 장소 새로고침 (`/api/places`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/places/{placeId}/refresh` | 전체 새로고침 (이미지+리뷰) | - |
| POST | `/api/places/{placeId}/refresh/images` | 이미지만 새로고침 | - |
| POST | `/api/places/{placeId}/refresh/reviews` | 리뷰만 새로고침 | - |
| POST | `/api/places/{placeId}/refresh/business-hours` | 영업시간 새로고침 | - |
| POST | `/api/places/{placeId}/refresh/menus` | 메뉴 새로고침 | - |
| POST | `/api/places/refresh/all` | 전체 비동기 새로고침 | - |
| POST | `/api/places/refresh/batch` | 배치 새로고침 (offset/limit) | - |

### 북마크 (`/api/bookmarks`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/bookmarks/toggle` | 북마크 토글 | USER |
| POST | `/api/bookmarks` | 북마크 추가 | USER |
| DELETE | `/api/bookmarks/{placeId}` | 북마크 삭제 | USER |
| GET | `/api/bookmarks` | 북마크 목록 (페이지네이션) | USER |
| GET | `/api/bookmarks/{placeId}` | 북마크 여부 확인 | USER |

### 댓글 (`/api`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/places/{placeId}/comments` | 댓글 작성 | USER |
| GET | `/api/places/{placeId}/comments` | 댓글 목록 (페이지네이션) | - |
| GET | `/api/user/comments` | 내 댓글 목록 | USER |
| PUT | `/api/comments/{commentId}` | 댓글 수정 (작성자만) | USER |
| DELETE | `/api/comments/{commentId}` | 댓글 삭제 (작성자만) | USER |

### 추천 (`/api/recommendations`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/recommendations/enhanced` | MBTI 가중 유사도 추천 | USER |
| GET | `/api/recommendations/mbti/{mbtiType}` | 특정 MBTI 타입 추천 | USER |
| GET | `/api/recommendations/explanation` | 추천 알고리즘 설명 | USER |
| GET | `/api/recommendations/good-to-visit` | 지금 가기 좋은 장소 (날씨/시간) | - |
| GET | `/api/recommendations/contextual` | 컨텍스트 기반 추천 (듀얼 모드) | - |
| POST | `/api/recommendations/query` | 레거시 쿼리 추천 | - |
| GET | `/api/recommendations/current-time` | 현재 시간 기반 추천 | - |
| GET | `/api/recommendations/bookmark-based` | 북마크 기반 추천 | - |

### 검색 (`/api/search`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/search` | 통합 검색 (임베딩+키워드) | - |
| GET | `/api/search/food` | 음식 전문 검색 | - |
| GET | `/api/search/activity` | 활동/목적 기반 검색 | - |
| GET | `/api/search/location` | 지역명 검색 | - |

### 카테고리 (`/api/categories`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/categories/suggested` | 날씨/시간 기반 추천 카테고리 | - |
| GET | `/api/categories/{category}/places` | 카테고리별 장소 (거리 가중) | - |

### 컨텍스트 추천 (`/api/contextual-recommendations`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/contextual-recommendations/weather-based` | 날씨 기반 추천 | - |

### 키워드 추천 (`/api/keyword-recommendations`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/keyword-recommendations/by-keyword` | 키워드 기반 추천 | USER |

### 위치 (`/api/location`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/location/reverse-geocode?lat=&lng=` | 좌표→주소 변환 (Vworld API) | - |
| POST | `/api/location/register-user-area?lat=&lng=` | 사용자 위치 등록 (크롤링 우선순위 큐) | - |

### 주소 (`/api/address`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/address/reverse` | 좌표→주소 (Naver Reverse Geocoding) | - |
| GET | `/api/address/test` | 주소 서비스 테스트 | - |

### 날씨 (`/api/weather`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/weather/current` | 현재 날씨 (기상청/OpenMeteo) | - |
| GET | `/api/weather/test` | 날씨 서비스 테스트 | - |

### 한국 행정구역 (`/api/korean-regions`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/korean-regions/all` | 전체 행정구역 조회 | - |
| GET | `/api/korean-regions/dong-level` | 동/읍/면 단위 조회 | - |
| GET | `/api/korean-regions/search-locations` | 검색용 지역명 조회 | - |
| GET | `/api/korean-regions/by-sido` | 시도별 행정구역 조회 | - |
| POST | `/api/korean-regions/clear-cache` | 캐시 초기화 | - |
| GET | `/api/korean-regions/cache-status` | 캐시 상태 확인 | - |

### 벡터 (`/api/vector`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/vector/user/regenerate` | 사용자 선호 벡터 재생성 | USER |
| POST | `/api/vector/place/{placeId}/regenerate` | 장소 벡터 재생성 | ADMIN |
| GET | `/api/vector/similarity/places` | 벡터 기반 추천 | USER |
| POST | `/api/vector/similarity/calculate` | 유사도 점수 계산 | USER |

### 약관 (`/api/terms`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/terms` | 약관 목록 | - |
| GET | `/api/terms/{termsId}` | 약관 전문 | - |

### 지원 (`/api/support`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/support/contact` | 문의/피드백 제출 | USER |

### 홈 (`/api/home`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/home/images` | 홈 화면 장소 이미지 (20개) | - |

### 앱 (`/api/app`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/app/version` | 앱 버전 정보 | - |

### 헬스체크

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/health` | 서버 헬스체크 | - |

### 배치 작업 (`/api/batch/jobs`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/batch/jobs/place-collection` | 장소 수집 배치 시작 | - |
| POST | `/api/batch/jobs/place-collection/{region}` | 특정 지역 장소 수집 | - |
| POST | `/api/batch/jobs/update-crawled-data` | 크롤링 데이터 보강 | - |
| POST | `/api/batch/jobs/vector-embedding` | 벡터 임베딩 생성 | - |
| GET | `/api/batch/jobs/running` | 실행 중인 배치 목록 | - |
| POST | `/api/batch/jobs/stop/{executionId}` | 특정 배치 중지 | - |
| POST | `/api/batch/jobs/stop-all` | 모든 배치 중지 | - |
| POST | `/api/batch/jobs/image-update` | 이미지 업데이트 | - |
| POST | `/api/batch/jobs/image-refresh` | 이미지 새로고침 (모드별) | - |

### 임베딩 배치 (`/api/batch/embeddings`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/batch/embeddings/run` | 키워드 임베딩 배치 실행 | - |
| GET | `/api/batch/embeddings/stats` | 임베딩 통계 | - |
| DELETE | `/api/batch/embeddings/place/{placeId}` | 장소 임베딩 삭제 | - |
| GET | `/api/batch/embeddings/health` | 임베딩 서비스 헬스 | - |

### 관리자 모니터링 (`/api/admin/monitor`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/admin/monitor/dashboard` | 대시보드 통계 | ADMIN |
| GET | `/api/admin/monitor/batch/servers` | 배치 서버 목록 | ADMIN |
| GET | `/api/admin/monitor/places/stats` | 장소 통계 | ADMIN |
| GET | `/api/admin/monitor/places/search` | 장소 검색 (필터) | ADMIN |
| GET | `/api/admin/monitor/batch/stats` | 큐 통계 | ADMIN |
| GET | `/api/admin/monitor/batch/stats/{serverName}` | 서버별 큐 통계 | ADMIN |
| GET | `/api/admin/monitor/batch/workers` | 워커 목록 | ADMIN |
| GET | `/api/admin/monitor/batch/workers/{serverName}` | 서버별 워커 | ADMIN |
| POST | `/api/admin/monitor/batch/worker/start` | 워커 시작 | ADMIN |
| POST | `/api/admin/monitor/batch/worker/start/{serverName}` | 서버별 워커 시작 | ADMIN |
| POST | `/api/admin/monitor/batch/worker/stop` | 워커 중지 | ADMIN |
| POST | `/api/admin/monitor/batch/worker/stop/{serverName}` | 서버별 워커 중지 | ADMIN |
| POST | `/api/admin/monitor/batch/push-all` | 전체 큐 등록 | ADMIN |
| POST | `/api/admin/monitor/batch/push-all/{serverName}` | 서버별 큐 등록 | ADMIN |
| GET | `/api/admin/monitor/batch/execute/{serverName}` | 배치 엔드포인트 프록시 | ADMIN |
| GET | `/api/admin/monitor/docker/containers` | Docker 컨테이너 목록 | ADMIN |
| GET | `/api/admin/monitor/docker/containers/{serverName}` | 서버별 컨테이너 | ADMIN |
| GET | `/api/admin/monitor/docker/logs/{containerName}` | Docker 로그 | ADMIN |
| GET | `/api/admin/monitor/docker/logs/{serverName}/{containerName}` | 서버별 로그 | ADMIN |
| GET | `/api/admin/monitor/batch/config/{serverName}` | 서버 설정 | ADMIN |
| GET | `/api/admin/monitor/batch/current-jobs/{serverName}` | 실행 중 작업 | ADMIN |
| GET | `/api/admin/monitor/crawling/map` | 크롤링 지도 시각화 데이터 | ADMIN |
| POST | `/api/admin/monitor/crawling/start-queue` | 전국 큐 기반 크롤링 시작 | ADMIN |

### 관리자 기능

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/admin/place-management/check-availability` | 장소 데이터 가용성 확인 | ADMIN |
| POST | `/api/admin/place-management/fetch` | 외부 API 장소 수집 | ADMIN |
| POST | `/api/admin/place-management/cleanup` | 저평점 장소 정리 | ADMIN |
| POST | `/api/place-enhancement/place/{placeId}/enhance` | 장소 정보 보강 | ADMIN |
| POST | `/api/place-enhancement/batch-enhance` | 일괄 장소 보강 | ADMIN |
| POST | `/api/admin/similarity/calculate` | 유사도 전체 계산 | ADMIN |
| POST | `/api/admin/similarity/refresh-topk` | Top-K 유사도 갱신 | ADMIN |
| POST | `/api/admin/similarity/refresh-topk/{placeId}` | 단일 장소 Top-K | ADMIN |
| POST | `/api/admin/similarity/calculate-pair/{id1}/{id2}` | 쌍별 유사도 계산 | ADMIN |
| GET | `/api/admin/similarity/status` | 유사도 계산 상태 | ADMIN |
| GET | `/api/admin/similarity/statistics` | 유사도 통계 | ADMIN |
| POST | `/api/images/place/{placeId}/fetch` | 이미지 수집 | ADMIN |
| POST | `/api/images/place/{placeId}/generate` | Gemini 이미지 생성 | ADMIN |
| POST | `/api/images/upload` | 이미지 직접 업로드 | - |
| POST | `/api/images/upload-from-urls` | URL 이미지 저장 | - |
| DELETE | `/api/images/{placeId}` | 장소 이미지 삭제 | - |
| POST | `/api/email/send` | 이메일 발송 | ADMIN |
| POST | `/api/email/test` | 테스트 이메일 | ADMIN |

## 배치 작업 파이프라인

```
Stage 1: 장소 수집 (Kakao/Tour API → DB)
  ↓ crawl_status=PENDING
Stage 2: 상세 크롤링 (Naver Selenium → 설명/이미지/리뷰)
  ↓ crawl_status=COMPLETED, embed_status=PENDING
Stage 3: 벡터 임베딩 (Kanana 2.1B → pgvector)
  ↓ embed_status=COMPLETED
→ 추천 엔진 사용 가능
```

## 문서

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

## 작성자

**Andrew Lim (임석현)** - sjsh1623@gmail.com
