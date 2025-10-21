package com.mohe.spring.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        String description = """
            # MOHE (모헤) API 문서

            한국의 숨은 장소를 발견하고 MBTI 기반 개인화 추천을 제공하는 REST API입니다.

            ## 주요 기능

            ### 🔐 인증 및 사용자 관리
            - **JWT 기반 인증**: 액세스 토큰(1시간) + 리프레시 토큰(30일)
            - **회원가입 절차**: 이메일 입력 → OTP 인증 → 닉네임/비밀번호 설정
            - **프로필 관리**: MBTI 유형, 연령대, 공간 선호도, 교통수단 설정

            ### 📍 장소 추천 시스템
            - **MBTI 기반 추천**: 사용자의 성격 유형에 맞는 장소 추천
            - **벡터 유사도 추천**: 북마크 이력을 분석한 개인화 추천
            - **키워드 추천**: 특정 키워드와 관련된 장소 검색
            - **상황별 추천**: 현재 시간대와 위치를 고려한 추천

            ### 🗂️ 장소 관리
            - **장소 검색**: 키워드, 카테고리, 위치 기반 검색
            - **장소 상세 정보**: 평점, 리뷰, 이미지, 영업시간, 교통 정보
            - **북마크 기능**: 관심 장소를 저장하고 관리
            - **최근 본 장소**: 사용자의 조회 이력 추적

            ### 🔄 배치 작업 (관리자)
            - **장소 데이터 수집**: Naver/Kakao API를 통한 대량 데이터 수집
            - **크롤링 업데이트**: 웹 크롤러를 통한 상세 정보 및 이미지 수집
            - **벡터 임베딩**: AI 기반 장소 설명 생성 및 키워드 추출

            ## 🔑 인증 사용 방법

            1. **로그인하기**
               - `POST /api/auth/login` API를 호출하여 이메일/비밀번호 전송
               - 응답에서 `accessToken` 값을 복사

            2. **토큰 등록하기**
               - Swagger UI 우측 상단의 **Authorize** 버튼 클릭
               - Value 필드에 `Bearer {복사한_토큰}` 형식으로 입력
               - 예시: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

            3. **API 테스트하기**
               - 🔒 자물쇠 아이콘이 있는 API는 인증이 필요한 엔드포인트입니다
               - Authorize 후에는 모든 인증 API를 자유롭게 테스트할 수 있습니다

            ## 📦 API 응답 형식

            모든 API는 다음과 같은 표준화된 형식으로 응답합니다:

            **성공 응답**
            ```json
            {
              "success": true,
              "data": {
                // 실제 데이터
              },
              "timestamp": "2025-10-20T12:00:00Z"
            }
            ```

            **에러 응답**
            ```json
            {
              "success": false,
              "error": {
                "code": "ERROR_CODE",
                "message": "에러 메시지",
                "path": "/api/endpoint"
              },
              "timestamp": "2025-10-20T12:00:00Z"
            }
            ```

            ## 🔄 주요 데이터 흐름

            ### 회원가입 프로세스
            1. `POST /api/auth/signup` - 이메일로 회원가입 시작 (OTP 발송)
            2. `POST /api/auth/verify-email` - OTP 코드 인증
            3. `POST /api/auth/setup-password` - 닉네임 및 비밀번호 설정
            4. `PUT /api/user/preferences` - MBTI 및 선호도 설정 (온보딩)

            ### 장소 탐색 프로세스
            1. `GET /api/places/search` - 키워드로 장소 검색
            2. `GET /api/places/{id}` - 장소 상세 정보 조회
            3. `POST /api/bookmarks/toggle` - 마음에 드는 장소 북마크
            4. `GET /api/recommendations/enhanced` - 개인화된 추천 받기

            ### 개인화 프로세스
            1. 사용자가 여러 장소를 북마크
            2. 시스템이 북마크 패턴을 분석하여 사용자 벡터 생성
            3. 사용자 벡터와 장소 벡터의 유사도 계산
            4. MBTI 가중치를 적용한 맞춤형 추천 제공

            ## 📞 문의 및 지원

            - **개발팀 이메일**: dev@mohe.app
            - **GitHub 저장소**: [MoheServer](https://github.com/sjsh1623/MoheServer)
            - **버전**: 1.0.0
            """;

        return new OpenAPI()
            .info(new Info()
                .title("MOHE Spring Boot API")
                .description(description)
                .version("1.0.0")
                .contact(new Contact()
                    .name("MOHE Development Team")
                    .email("dev@mohe.app")
                )
            )
            .servers(List.of(
                new Server().url("http://localhost:8000").description("Local Development Server"),
                new Server().url("https://api.mohe.app").description("Production Server")
            ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT 토큰을 사용한 인증. 헤더에 'Bearer {token}' 형식으로 전송")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}