package com.mohe.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.dto.*;
import com.mohe.spring.security.JwtTokenProvider;
import com.mohe.spring.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("사용자 컨트롤러 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private UserPreferencesRequest preferencesRequest;
    private ProfileEditRequest profileEditRequest;
    private UserDto.AgreementsRequest agreementsRequest;
    private UserDto.OnboardingCompleteRequest onboardingRequest;

    @BeforeEach
    void setUp() {
        // 선호도 설정 요청 데이터
        MbtiPreference mbti = new MbtiPreference("E", "N", "T", "J");
        preferencesRequest = new UserPreferencesRequest(
            mbti,
            "20",
            List.of("workshop", "exhibition", "nature"),
            "public"
        );

        // 프로필 수정 요청 데이터
        profileEditRequest = new ProfileEditRequest(
            "newnickname",
            "https://example.com/profile.jpg"
        );

        // 약관 동의 요청 데이터
        agreementsRequest = new UserDto.AgreementsRequest(
            true, true, true, true
        );

        // 온보딩 완료 요청 데이터
        onboardingRequest = new UserDto.OnboardingCompleteRequest(1L);
    }

    @Test
    @DisplayName("사용자 선호도 설정 성공 테스트")
    @WithMockUser(roles = "USER")
    void testUpdatePreferencesSuccess() throws Exception {
        // Given
        UserPreferencesResponse response = new UserPreferencesResponse(
            new UserPreferencesData(
                "ENTJ",
                "20",
                List.of("workshop", "exhibition", "nature"),
                "public"
            )
        );
        when(userService.updatePreferences(any(UserPreferencesRequest.class)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/user/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferencesRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("선호도 설정이 완료되었습니다."))
                .andExpect(jsonPath("$.data.preferences.mbti").value("ENTJ"))
                .andExpect(jsonPath("$.data.preferences.ageRange").value("20"));
    }

    @Test
    @DisplayName("사용자 프로필 조회 성공 테스트")
    @WithMockUser(roles = "USER")
    void testGetUserProfileSuccess() throws Exception {
        // Given
        UserProfileResponse response = new UserProfileResponse(
            new UserProfileData(
                "1",
                "user@example.com",
                "testnick",
                "ENTJ",
                "20",
                List.of("workshop", "exhibition"),
                "public",
                "https://example.com/profile.jpg",
                null
            )
        );
        when(userService.getUserProfile()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/user/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.user.nickname").value("testnick"))
                .andExpect(jsonPath("$.data.user.mbti").value("ENTJ"));
    }

    @Test
    @DisplayName("프로필 수정 성공 테스트")
    @WithMockUser(roles = "USER")
    void testEditProfileSuccess() throws Exception {
        // Given
        ProfileEditResponse response = new ProfileEditResponse(
            new ProfileEditData(
                "1",
                "newnickname",
                "https://example.com/profile.jpg"
            )
        );
        when(userService.editProfile(any(ProfileEditRequest.class)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/user/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileEditRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("프로필이 성공적으로 수정되었습니다."))
                .andExpect(jsonPath("$.data.user.nickname").value("newnickname"));
    }

    @Test
    @DisplayName("프로필 수정 실패 테스트 - 중복 닉네임")
    @WithMockUser(roles = "USER")
    void testEditProfileFailureDuplicateNickname() throws Exception {
        // Given
        when(userService.editProfile(any(ProfileEditRequest.class)))
            .thenThrow(new RuntimeException("이미 사용 중인 닉네임입니다"));

        // When & Then
        mockMvc.perform(put("/api/user/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileEditRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_NICKNAME"));
    }

    @Test
    @DisplayName("약관 동의 저장 성공 테스트")
    @WithMockUser(roles = "USER")
    void testSaveAgreementsSuccess() throws Exception {
        // Given
        UserDto.AgreementsResponse response = new UserDto.AgreementsResponse(
            "약관 동의 완료"
        );
        when(userService.saveAgreements(any(UserDto.AgreementsRequest.class)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/user/agreements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(agreementsRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("약관 동의 완료"));
    }

    @Test
    @DisplayName("온보딩 완료 처리 성공 테스트")
    @WithMockUser(roles = "USER")
    void testCompleteOnboardingSuccess() throws Exception {
        // Given
        UserDto.OnboardingCompleteResponse response = new UserDto.OnboardingCompleteResponse(
            "온보딩 완료"
        );
        when(userService.completeOnboarding(any(UserDto.OnboardingCompleteRequest.class)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/user/onboarding/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(onboardingRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("온보딩 완료"));
    }

    @Test
    @DisplayName("인증 없이 프로필 조회 실패 테스트")
    void testGetUserProfileWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/user/profile"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
