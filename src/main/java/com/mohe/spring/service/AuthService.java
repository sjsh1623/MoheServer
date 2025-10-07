package com.mohe.spring.service;

import com.mohe.spring.dto.*;
import com.mohe.spring.entity.*;
import com.mohe.spring.repository.*;
import com.mohe.spring.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@Transactional
public class AuthService {
    
    private final UserRepository userRepository;
    private final TempUserRepository tempUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Optional<EmailService> emailService;
    private final Random random = new Random();

    public AuthService(
            UserRepository userRepository,
            TempUserRepository tempUserRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            Optional<EmailService> emailService
    ) {
        this.userRepository = userRepository;
        this.tempUserRepository = tempUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }
    
    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        // Authenticate using the user's email and password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Update last login time
        userRepository.updateLastLoginAt(user.getId(), OffsetDateTime.now());
        
        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        
        // Save refresh token
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000));
        refreshTokenRepository.save(refreshTokenEntity);
        
        UserInfo userInfo = new UserInfo(
            user.getId().toString(),
            user.getEmail(),
            user.getNickname(),
            user.getIsOnboardingCompleted(),
            List.of("ROLE_USER")
        );
        
        return new LoginResponse(
            userInfo,
            accessToken,
            refreshToken,
            (int) (jwtTokenProvider.getAccessTokenExpiration() / 1000)
        );
    }
    
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }
        
        // Delete existing temp user with same email
        Optional<TempUser> existingTempUser = tempUserRepository.findByEmail(request.getEmail());
        existingTempUser.ifPresent(tempUserRepository::delete);
        
        String tempUserId = UUID.randomUUID().toString();
        String verificationCode = generateOTPCode();
        
        TempUser tempUser = new TempUser();
        tempUser.setId(tempUserId);
        tempUser.setEmail(request.getEmail());
        tempUser.setVerificationCode(verificationCode);
        tempUser.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        
        tempUserRepository.save(tempUser);
        
        // Skip actual email sending as per requirements
        // emailService.sendVerificationEmail(request.getEmail(), verificationCode);
        
        return new SignupResponse(tempUserId);
    }
    
    public EmailVerificationResponse verifyEmail(EmailVerificationRequest request) {
        TempUser tempUser = tempUserRepository.findValidTempUser(request.getTempUserId(), OffsetDateTime.now())
                .orElseThrow(() -> new RuntimeException("유효하지 않은 인증 요청입니다"));
        
        // Accept bypass code "00000" or the actual verification code
        if (!request.getOtpCode().equals("00000") && !tempUser.getVerificationCode().equals(request.getOtpCode())) {
            throw new RuntimeException("인증 코드가 일치하지 않습니다");
        }
        
        return new EmailVerificationResponse(true);
    }
    
    public NicknameCheckResponse checkNickname(NicknameCheckRequest request) {
        boolean isAvailable = !userRepository.existsByNickname(request.getNickname());
        
        return new NicknameCheckResponse(
            isAvailable,
            isAvailable ? "사용 가능한 닉네임입니다" : "이미 사용중인 닉네임입니다"
        );
    }
    
    public LoginResponse setupPassword(PasswordSetupRequest request) {
        TempUser tempUser = tempUserRepository.findValidTempUser(request.getTempUserId(), OffsetDateTime.now())
                .orElseThrow(() -> new RuntimeException("유효하지 않은 회원가입 요청입니다"));
        
        if (!request.isTermsAgreed()) {
            throw new RuntimeException("약관 동의가 필요합니다");
        }
        
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new RuntimeException("이미 사용중인 닉네임입니다");
        }
        
        User user = new User();
        user.setEmail(tempUser.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setIsOnboardingCompleted(false);
        
        User savedUser = userRepository.save(user);
        
        // Delete temp user
        tempUserRepository.delete(tempUser);
        
        // Generate tokens for immediate login
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
        );
        
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId());
        
        // Save refresh token
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(savedUser);
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000));
        refreshTokenRepository.save(refreshTokenEntity);
        
        UserInfo userInfo = new UserInfo(
            savedUser.getId().toString(),
            savedUser.getEmail(),
            savedUser.getNickname(),
            savedUser.getIsOnboardingCompleted(),
            List.of("ROLE_USER")
        );
        
        return new LoginResponse(
            userInfo,
            accessToken,
            refreshToken,
            (int) (jwtTokenProvider.getAccessTokenExpiration() / 1000)
        );
    }
    
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findValidToken(request.getRefreshToken(), OffsetDateTime.now())
                .orElseThrow(() -> new RuntimeException("유효하지 않은 리프레시 토큰입니다"));
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                refreshToken.getUser().getEmail(),
                null,
                List.of()
        );
        
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        
        return new TokenRefreshResponse(
            accessToken,
            "Bearer",
            (int) (jwtTokenProvider.getAccessTokenExpiration() / 1000)
        );
    }
    
    public void logout(LogoutRequest request) {
        refreshTokenRepository.revokeByToken(request.getRefreshToken());
        SecurityContextHolder.clearContext();
    }
    
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("등록되지 않은 이메일입니다"));
        
        String resetToken = UUID.randomUUID().toString();
        
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setUser(user);
        passwordResetToken.setToken(resetToken);
        passwordResetToken.setExpiresAt(OffsetDateTime.now().plusHours(1));
        
        passwordResetTokenRepository.save(passwordResetToken);
        
        // Skip actual email sending as per requirements
        // emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }
    
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findValidToken(request.getToken(), OffsetDateTime.now())
                .orElseThrow(() -> new RuntimeException("유효하지 않은 리셋 토큰입니다"));
        
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Mark token as used
        passwordResetTokenRepository.markTokenAsUsed(request.getToken());
        
        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }
    
    private String generateOTPCode() {
        return String.format("%05d", random.nextInt(100000));
    }
}