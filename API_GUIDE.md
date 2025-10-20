# API 가이드

Swagger UI(`http://localhost:8080/swagger-ui.html`)와 OpenAPI 스펙(`http://localhost:8080/v3/api-docs`)에서 전체 스키마를 확인할 수 있습니다. 아래 표는 주요 컨트롤러별 핵심 엔드포인트를 한눈에 정리한 것입니다. `권한` 열은 실제 코드의 `@PreAuthorize` 설정을 기준으로 하며, 명시되지 않은 엔드포인트는 현재 인증이 필요하지 않습니다.

## 인증 및 온보딩
| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 공개 | 이메일/비밀번호로 로그인하고 액세스/리프레시 토큰을 발급합니다. |
| POST | `/api/auth/signup` | 공개 | 회원가입을 시작하고 이메일 OTP를 발송합니다. |
| POST | `/api/auth/verify-email` | 공개 | 5자리 OTP를 검증하여 임시 계정을 활성화합니다. |
| POST | `/api/auth/check-nickname` | 공개 | 닉네임 사용 가능 여부를 검사합니다. |
| POST | `/api/auth/setup-password` | 공개 | 이메일 인증을 마친 사용자의 비밀번호를 설정합니다. |
| POST | `/api/auth/refresh` | 공개 | 리프레시 토큰으로 새로운 액세스 토큰을 발급합니다. |
| POST | `/api/auth/logout` | 공개 | 리프레시 토큰을 무효화합니다. |
| POST | `/api/auth/forgot-password` | 공개 | 비밀번호 재설정 메일을 발송합니다. |
| POST | `/api/auth/reset-password` | 공개 | 재설정 토큰으로 비밀번호를 변경합니다. |
| GET | `/api/terms` | 공개 | 온보딩 시 필요한 필수/선택 약관 목록을 반환합니다. |
| GET | `/api/terms/{termsId}` | 공개 | 특정 약관 전문(placeholder)을 조회합니다. |

## 사용자 & 활동
| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| PUT | `/api/user/preferences` | Bearer (ROLE_USER) | MBTI, 연령대, 공간/이동 선호도를 저장합니다. |
| GET | `/api/user/profile` | Bearer (ROLE_USER) | 로그인한 사용자의 프로필을 조회합니다. |
| PUT | `/api/user/profile` | Bearer (ROLE_USER) | 닉네임, 프로필 이미지를 수정합니다. |
| POST | `/api/user/agreements` | Bearer (ROLE_USER) | 이용약관, 개인정보, 위치정보, 연령 확인 동의를 저장합니다. |
| POST | `/api/user/onboarding/complete` | Bearer (ROLE_USER) | 모든 온보딩 과정을 완료하고 상태를 업데이트합니다. |
| GET | `/api/user/recent-places` | Bearer (ROLE_USER) | 최근에 조회한 장소 목록을 반환합니다. |
| POST | `/api/user/recent-places` | Bearer (ROLE_USER) | 장소 상세 조회 시 최근 이력으로 기록합니다. |
| GET | `/api/user/my-places` | Bearer (ROLE_USER) | 사용자가 직접 등록한 장소 목록을 조회합니다. |

## 북마크
| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/bookmarks/toggle` | Bearer (ROLE_USER) | 지정된 장소의 북마크를 토글합니다. |
| POST | `/api/bookmarks` | Bearer (ROLE_USER) | 북마크를 강제로 추가합니다. |
| DELETE | `/api/bookmarks/{placeId}` | Bearer (ROLE_USER) | 북마크를 제거합니다. |
| GET | `/api/bookmarks` | Bearer (ROLE_USER) | 페이징된 북마크 목록을 가져옵니다. |
| GET | `/api/bookmarks/{placeId}` | Bearer (ROLE_USER) | 특정 장소의 북마크 여부를 확인합니다. |

## 장소 탐색 & 유틸리티
| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/places/recommendations` | 공개 | 게스트/회원 구분 없이 추천 장소를 제공합니다. |
| GET | `/api/places/new` | 공개 | 새로운 추천 장소 리스트를 갱신하여 반환합니다(추천 API와 동일). |
| GET | `/api/places` | 공개 | 페이지네이션, 정렬, 카테고리 필터가 있는 장소 목록을 조회합니다. |
| GET | `/api/places/{id}` | 공개 | 장소 상세 정보와 조회 이력을 반환합니다. |
| GET | `/api/places/search` | 공개 | 검색어와 날씨/시간 컨텍스트 기반으로 장소를 검색합니다. |
| GET | `/api/places/nearby` | 공개 | 위경도와 반경을 기준으로 주변 인기 장소를 조회합니다. |
| GET | `/api/places/debug` | 공개 | 데이터 점검용 디버그 정보를 제공합니다. |
| GET | `/api/places/popular` | 공개 | 20km 이내 인기 장소를 리뷰/평점 기준으로 반환합니다. |
| GET | `/api/places/current-time` | 공개 | 현재 시간과 날씨 기반 추천을 제공합니다. |
| GET | `/api/places/list` | 공개 | 0페이지 기반 목록을 조회합니다(관리/내부용). |
| GET | `/api/places/vector-search` | Bearer (ROLE_USER) | 벡터 유사도를 활용한 개인화 검색을 수행합니다. |
| GET | `/api/home/images` | 공개 | 홈 화면에 쓰이는 대표 장소 이미지 목록을 반환합니다. |
| GET | `/api/address/reverse` | 공개 | 위경도를 한국 행정 주소로 변환합니다. |
| GET | `/api/address/test` | 공개 | 주소 서비스 헬스체크용 테스트 응답을 제공합니다. |
| GET | `/api/weather/current` | 공개 | 지정 위치의 현재 날씨를 조회합니다. |
| GET | `/health` | 공개 | 애플리케이션 헬스 상태를 확인합니다. |

## 추천 서비스
| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/recommendations/enhanced` | Bearer (ROLE_USER) | 북마크 + MBTI 가중치를 반영한 개인화 추천을 제공합니다. |
| GET | `/api/recommendations/mbti/{mbtiType}` | Bearer (ROLE_USER) | 특정 MBTI에 맞춘 추천 리스트를 반환합니다. |
| GET | `/api/recommendations/explanation` | Bearer (ROLE_USER) | 추천 결과가 생성된 이유를 설명합니다. |
| GET | `/api/recommendations/contextual` | 공개 | 위경도/쿼리를 받아 상황별 추천을 생성합니다(로그인 시 북마크·MBTI 반영). |
| POST | `/api/recommendations/query` | 공개 | 레거시 POST 입력을 받아 상황별 추천 엔드포인트를 위임 호출합니다. |
| GET | `/api/recommendations/current-time` | 공개 | 현재 시간대 추천(PlaceController와 동일 로직)을 제공합니다. |
| GET | `/api/recommendations/bookmark-based` | 공개 | 위치 기반 북마크 인기 장소를 제공합니다. |
| GET | `/api/contextual-recommendations/weather-based` | 공개 | 날씨 정보만으로 최적 장소를 추천합니다. |
| GET | `/api/keyword-recommendations/by-keyword` | Bearer (ROLE_USER) | 키워드와 유사한 장소 목록을 반환합니다. |

## 행정구역 & 지역 데이터
| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/korean-regions/all` | 공개 | 행정안전부 API에서 전체 행정구역 목록을 임시 조회합니다. |
| GET | `/api/korean-regions/dong-level` | 공개 | 동/읍/면 단위 행정구역만 필터링하여 반환합니다. |
| GET | `/api/korean-regions/search-locations` | 공개 | 검색에 활용 가능한 지역명 목록을 제공합니다. |
| GET | `/api/korean-regions/by-sido` | 공개 | 시도 코드별 행정구역을 필터링합니다. |
| POST | `/api/korean-regions/clear-cache` | 공개 | 지역 데이터 캐시를 비웁니다(인증 없음 주의). |
| GET | `/api/korean-regions/cache-status` | 공개 | 캐시 현황과 데이터 출처를 확인합니다. |

## 앱 정보 & 고객 지원
| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/app/version` | 공개 | 현재 앱 버전과 출시일을 조회합니다. |
| POST | `/api/support/contact` | Bearer (ROLE_USER) | 사용자 문의 또는 피드백을 접수합니다. |

## 벡터 & 유사도 관리
| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/vector/user/regenerate` | Bearer (ROLE_USER) | 현재 사용자의 선호도 벡터를 재생성합니다. |
| POST | `/api/vector/place/{placeId}/regenerate` | Bearer (ROLE_ADMIN) | 특정 장소 설명을 기반으로 벡터를 재계산합니다. |
| GET | `/api/vector/similarity/places` | Bearer (ROLE_USER) | 사용자 벡터 기준 Top-N 장소를 조회합니다. |
| POST | `/api/vector/similarity/calculate` | Bearer (ROLE_USER) | 사용자-장소 간 유사도를 계산하고 캐시에 반영합니다. |
| POST | `/api/admin/similarity/calculate` | Bearer (ROLE_ADMIN) | 전체 장소 유사도 재계산을 비동기로 트리거합니다. |
| POST | `/api/admin/similarity/refresh-topk` | Bearer (ROLE_ADMIN) | 다수 장소의 Top-K 유사도 캐시를 갱신합니다. |
| POST | `/api/admin/similarity/refresh-topk/{placeId}` | Bearer (ROLE_ADMIN) | 단일 장소의 Top-K 유사도를 갱신합니다. |
| POST | `/api/admin/similarity/calculate-pair/{id1}/{id2}` | Bearer (ROLE_ADMIN) | 두 장소 간 유사도를 즉시 계산합니다. |
| GET | `/api/admin/similarity/status` | Bearer (ROLE_ADMIN) | 유사도 작업 실행 상태와 통계를 확인합니다. |
| GET | `/api/admin/similarity/statistics` | Bearer (ROLE_ADMIN) | 유사도 시스템의 상세 통계를 반환합니다. |

## 관리자 도구 & 배치
| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/admin/place-management/check-availability` | Bearer (ROLE_ADMIN) | 추천에 필요한 최소 장소 수를 점검하고 필요 시 자동 수집을 실행합니다. |
| POST | `/api/admin/place-management/fetch` | Bearer (ROLE_ADMIN) | 외부 API로 신규 장소를 수집합니다. |
| POST | `/api/admin/place-management/cleanup` | Bearer (ROLE_ADMIN) | 오래된 저품질 장소 데이터를 정리합니다. |
| POST | `/api/place-enhancement/place/{placeId}/enhance` | Bearer (ROLE_ADMIN) | 단일 장소 정보를 외부 API로 강화합니다. |
| POST | `/api/place-enhancement/batch-enhance` | Bearer (ROLE_ADMIN) | 여러 장소의 정보를 일괄 보강합니다. |
| POST | `/api/images/place/{placeId}/fetch` | Bearer (ROLE_ADMIN) | 외부 API에서 장소 이미지를 가져옵니다. |
| POST | `/api/images/place/{placeId}/generate` | Bearer (ROLE_ADMIN) | Gemini를 이용해 장소 이미지를 생성합니다. |
| POST | `/api/email/send` | Bearer (ROLE_ADMIN) | HTML/텍스트 이메일을 발송합니다. |
| POST | `/api/email/test` | Bearer (ROLE_ADMIN) | 이메일 설정을 검증하기 위한 테스트 메일을 보냅니다. |
| POST | `/api/batch/jobs/place-collection` | 공개 | 장소 수집 배치 작업을 비동기로 시작합니다. |
| POST | `/api/batch/jobs/place-collection/{region}` | 공개 | 특정 지역에 한정해 장소 수집 배치를 실행합니다. |
| POST | `/api/batch/jobs/update-crawled-data` | 공개 | 크롤링된 장소 데이터를 최신으로 갱신합니다. |
| POST | `/api/batch/jobs/vector-embedding` | 공개 | 키워드 벡터 임베딩 배치 작업을 실행합니다. |
| GET | `/api/batch/jobs/running` | 공개 | 현재 실행 중인 모든 배치 작업 정보를 조회합니다. |
| POST | `/api/batch/jobs/stop/{executionId}` | 공개 | 특정 실행 ID의 배치 작업을 중지합니다. |
| POST | `/api/batch/jobs/stop-all` | 공개 | 실행 중인 모든 배치 작업을 일괄 중지합니다. |

---

> **보안 주의**: 일부 관리자/배치 엔드포인트는 현재 코드상 별도의 인증이 걸려 있지 않습니다. 운영 환경에서는 API Gateway 혹은 네트워크 레벨에서 반드시 추가 보호 장치를 구성하세요.
