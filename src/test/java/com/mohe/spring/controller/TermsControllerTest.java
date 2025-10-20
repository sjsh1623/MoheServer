package com.mohe.spring.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("약관 컨트롤러 테스트")
class TermsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("약관 목록 조회 성공 테스트")
    void testGetTermsListSuccess() throws Exception {
        mockMvc.perform(get("/api/terms"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.terms").isArray())
                .andExpect(jsonPath("$.data.terms[0].id").value("service-terms"))
                .andExpect(jsonPath("$.data.terms[0].title").value("서비스 이용약관 동의 (필수)"))
                .andExpect(jsonPath("$.data.terms[0].required").value(true));
    }

    @Test
    @DisplayName("약관 상세 조회 성공 테스트 - service-terms")
    void testGetTermsDetailServiceTerms() throws Exception {
        mockMvc.perform(get("/api/terms/service-terms"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("service-terms"))
                .andExpect(jsonPath("$.data.title").value("서비스 이용약관"))
                .andExpect(jsonPath("$.data.content").exists())
                .andExpect(jsonPath("$.data.lastUpdated").exists());
    }

    @Test
    @DisplayName("약관 상세 조회 성공 테스트 - privacy-policy")
    void testGetTermsDetailPrivacyPolicy() throws Exception {
        mockMvc.perform(get("/api/terms/privacy-policy"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("privacy-policy"))
                .andExpect(jsonPath("$.data.title").value("개인정보 수집 및 이용 동의"));
    }

    @Test
    @DisplayName("약관 상세 조회 성공 테스트 - location-terms")
    void testGetTermsDetailLocationTerms() throws Exception {
        mockMvc.perform(get("/api/terms/location-terms"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("location-terms"))
                .andExpect(jsonPath("$.data.title").value("위치 정보 이용약관"));
    }

    @Test
    @DisplayName("약관 상세 조회 성공 테스트 - age-verification")
    void testGetTermsDetailAgeVerification() throws Exception {
        mockMvc.perform(get("/api/terms/age-verification"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("age-verification"))
                .andExpect(jsonPath("$.data.title").value("연령 확인 안내"));
    }

    @Test
    @DisplayName("약관 상세 조회 - 알 수 없는 약관 ID")
    void testGetTermsDetailUnknownId() throws Exception {
        mockMvc.perform(get("/api/terms/unknown-terms"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("unknown-terms"))
                .andExpect(jsonPath("$.data.title").value("확인되지 않은 약관"));
    }
}
