package com.mohe.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.dto.*;
import com.mohe.spring.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("인증 컨트롤러 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private EmailVerificationRequest emailVerificationRequest;
    private NicknameCheckRequest nicknameCheckRequest;
    private PasswordSetupRequest passwordSetupRequest;
    private TokenRefreshRequest tokenRefreshRequest;
    private LogoutRequest logoutRequest;
    private ForgotPasswordRequest forgotPasswordRequest;
    private ResetPasswordRequest resetPasswordRequest;

    @BeforeEach
    void setUp() {
        // 로그인 요청 데이터
        loginRequest = new LoginRequest("test@mohe.com", "password123");

        // 회원가입 요청 데이터
        signupRequest = new SignupRequest("newuser@mohe.com");

        // 이메일 인증 요청 데이터
        emailVerificationRequest = new EmailVerificationRequest("newuser@mohe.com", "12345");

        // 닉네임 체크 요청 데이터
        nicknameCheckRequest = new NicknameCheckRequest("testnick");

        // 비밀번호 설정 요청 데이터
        passwordSetupRequest = new PasswordSetupRequest(
            "temp_user_123",
            "testnick",
            "password123",
            true
        );

        // 토큰 갱신 요청 데이터
        tokenRefreshRequest = new TokenRefreshRequest("refresh_token_here");

        // 로그아웃 요청 데이터
        logoutRequest = new LogoutRequest("refresh_token_here");

        // 비밀번호 재설정 요청 데이터
        forgotPasswordRequest = new ForgotPasswordRequest("test@mohe.com");
        resetPasswordRequest = new ResetPasswordRequest("reset_token", "NewPassword123!", "NewPassword123!");
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void testLoginSuccess() throws Exception {
        // Given
        UserInfo userInfo = new UserInfo("1", "test@mohe.com", "testnick", true, List.of("ROLE_USER"));
        LoginResponse loginResponse = new LoginResponse(
            userInfo,
            "access_token",
            "refresh_token",
            "Bearer",
            3600
        );
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("test@mohe.com"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 잘못된 인증 정보")
    void testLoginFailure() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다."));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void testSignupSuccess() throws Exception {
        // Given
        SignupResponse signupResponse = new SignupResponse("temp_user_123");
        when(authService.signup(any(SignupRequest.class))).thenReturn(signupResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("인증 코드가 이메일로 발송되었습니다."))
                .andExpect(jsonPath("$.data.tempUserId").exists());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 중복 이메일")
    void testSignupFailureDuplicateEmail() throws Exception {
        // Given
        when(authService.signup(any(SignupRequest.class)))
            .thenThrow(new RuntimeException("이미 사용 중인 이메일입니다"));

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    @DisplayName("이메일 인증 성공 테스트")
    void testVerifyEmailSuccess() throws Exception {
        // Given
        EmailVerificationResponse verificationResponse = new EmailVerificationResponse(true);
        when(authService.verifyEmail(any(EmailVerificationRequest.class)))
            .thenReturn(verificationResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailVerificationRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."))
                .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    @DisplayName("이메일 인증 실패 테스트 - 잘못된 인증 코드")
    void testVerifyEmailFailureInvalidCode() throws Exception {
        // Given
        when(authService.verifyEmail(any(EmailVerificationRequest.class)))
            .thenThrow(new RuntimeException("인증 코드가 일치하지 않습니다"));

        // When & Then
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailVerificationRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    @DisplayName("닉네임 중복 확인 성공 테스트")
    void testCheckNicknameSuccess() throws Exception {
        // Given
        NicknameCheckResponse nicknameCheckResponse = new NicknameCheckResponse(true, "사용 가능한 닉네임입니다.");
        when(authService.checkNickname(any(NicknameCheckRequest.class)))
            .thenReturn(nicknameCheckResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/check-nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nicknameCheckRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.message").value("사용 가능한 닉네임입니다."));
    }

    @Test
    @DisplayName("비밀번호 설정 및 회원가입 완료 테스트")
    void testSetupPasswordSuccess() throws Exception {
        // Given
        UserInfo userInfo = new UserInfo("1", "newuser@mohe.com", "testnick", true, List.of("ROLE_USER"));
        LoginResponse loginResponse = new LoginResponse(
            userInfo,
            "access_token",
            "refresh_token",
            "Bearer",
            3600
        );
        when(authService.setupPassword(any(PasswordSetupRequest.class))).thenReturn(loginResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/setup-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordSetupRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("토큰 갱신 성공 테스트")
    void testRefreshTokenSuccess() throws Exception {
        // Given
        TokenRefreshResponse tokenRefreshResponse = new TokenRefreshResponse(
            "new_access_token",
            "new_refresh_token",
            3600
        );
        when(authService.refreshToken(any(TokenRefreshRequest.class)))
            .thenReturn(tokenRefreshResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tokenRefreshRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access_token").value("new_access_token"));
    }

    @Test
    @DisplayName("토큰 갱신 실패 테스트 - 유효하지 않은 리프레시 토큰")
    void testRefreshTokenFailure() throws Exception {
        // Given
        when(authService.refreshToken(any(TokenRefreshRequest.class)))
            .thenThrow(new RuntimeException("유효하지 않은 리프레시 토큰입니다"));

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tokenRefreshRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("로그아웃 성공 테스트")
    void testLogoutSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."));
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 성공 테스트")
    void testForgotPasswordSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("비밀번호 재설정 링크가 이메일로 발송되었습니다."));
    }

    @Test
    @DisplayName("비밀번호 재설정 완료 성공 테스트")
    void testResetPasswordSuccess() throws Exception {
        // Given - Mock void method to do nothing (success case)
        Mockito.doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
