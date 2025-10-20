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
            - **JWT 기반 인증**: Access Token (1시간) + Refresh Token (30일)
            - **회원가입 절차**: 이메일 → OTP 인증 → 비밀번호 설정
            - **프로필 관리**: MBTI, 연령대, 공간 선호도 설정

            ### 📍 장소 추천 시스템
            - **MBTI 기반 추천**: 사용자의 성격 유형에 맞는 장소 추천
            - **벡터 유사도 추천**: 북마크 이력 기반 개인화 추천
            - **키워드 추천**: 특정 키워드로 장소 검색
            - **상황별 추천**: 시간대, 날씨를 고려한 추천

            ### 🗂️ 장소 관리
            - **장소 검색**: 키워드, 위치 기반 검색
            - **장소 상세**: 평점, 리뷰, 이미지, 영업시간 등
            - **북마크**: 관심 장소 저장 및 관리
            - **최근 본 장소**: 조회 이력 추적

            ### 🔄 배치 작업
            - **장소 수집**: Naver/Kakao API를 통한 장소 데이터 수집
            - **크롤링 업데이트**: 상세 정보 및 이미지 수집
            - **벡터 임베딩**: AI 기반 장소 설명 및 키워드 생성

            ## 인증 방법

            1. **로그인**: `POST /api/auth/login`으로 이메일/비밀번호 전송
            2. **토큰 획득**: 응답에서 `accessToken` 복사
            3. **Authorize 버튼 클릭**: 우측 상단의 🔓 버튼 클릭
            4. **토큰 입력**: `Bearer {your_access_token}` 형식으로 입력
            5. **API 테스트**: 이제 모든 인증이 필요한 API 사용 가능

            ## API 응답 형식

            모든 API는 표준화된 응답 형식을 사용합니다:

            ```json
            {
              "success": true,
              "data": { ... },
              "timestamp": "2025-10-20T12:00:00Z"
            }
            ```

            에러 응답:
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

            ## 데이터 흐름

            1. **회원가입**: 이메일 → OTP 인증 → 프로필 설정 → 온보딩 완료
            2. **장소 탐색**: 검색/추천 → 상세 조회 → 북마크 추가
            3. **개인화**: 북마크 축적 → 벡터 생성 → 맞춤 추천 제공

            ## 문의

            - **개발팀**: dev@mohe.app
            - **GitHub**: [MoheServer](https://github.com/sjsh1623/MoheServer)
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
                new Server().url("http://localhost:8080").description("Local Development Server"),
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