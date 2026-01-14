package com.mohe.spring.service;

import com.mohe.spring.dto.*;
import com.mohe.spring.entity.*;
import com.mohe.spring.repository.*;
import com.mohe.spring.security.JwtTokenProvider;
import com.mohe.spring.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional
public class SocialAuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${oauth.kakao.client-id:}")
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    @Value("${oauth.google.client-secret:}")
    private String googleClientSecret;

    public SocialAuthService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Handle Kakao OAuth login
     */
    public LoginResponse loginWithKakao(SocialLoginRequest request) {
        // Exchange code for tokens
        Map<String, Object> tokenResponse = exchangeKakaoCode(request.getCode(), request.getRedirectUri());
        String accessToken = (String) tokenResponse.get("access_token");

        // Get user info from Kakao
        Map<String, Object> userInfo = getKakaoUserInfo(accessToken);

        String kakaoId = String.valueOf(userInfo.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : null;

        return processOAuthLogin("kakao", kakaoId, email, nickname, accessToken,
                (String) tokenResponse.get("refresh_token"), tokenResponse.get("expires_in"));
    }

    /**
     * Handle Google OAuth login
     */
    public LoginResponse loginWithGoogle(SocialLoginRequest request) {
        // Exchange code for tokens
        Map<String, Object> tokenResponse = exchangeGoogleCode(request.getCode(), request.getRedirectUri());
        String accessToken = (String) tokenResponse.get("access_token");

        // Get user info from Google
        Map<String, Object> userInfo = getGoogleUserInfo(accessToken);

        String googleId = (String) userInfo.get("sub");
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");

        return processOAuthLogin("google", googleId, email, name, accessToken,
                (String) tokenResponse.get("refresh_token"), tokenResponse.get("expires_in"));
    }

    /**
     * Process OAuth login - find or create user
     */
    private LoginResponse processOAuthLogin(String provider, String providerId, String email,
                                            String name, String providerAccessToken,
                                            String providerRefreshToken, Object expiresIn) {

        // Check if social account already exists
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderId(provider, providerId);

        User user;
        boolean isNewUser = false;

        if (existingSocialAccount.isPresent()) {
            // Existing social account - get the user
            user = existingSocialAccount.get().getUser();

            // Update tokens
            SocialAccount socialAccount = existingSocialAccount.get();
            socialAccount.setAccessToken(providerAccessToken);
            if (providerRefreshToken != null) {
                socialAccount.setRefreshToken(providerRefreshToken);
            }
            if (expiresIn != null) {
                int seconds = expiresIn instanceof Integer ? (Integer) expiresIn : Integer.parseInt(expiresIn.toString());
                socialAccount.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(seconds));
            }
            socialAccountRepository.save(socialAccount);

        } else {
            // New social login - check if email already exists
            if (email != null) {
                Optional<User> existingUser = userRepository.findByEmail(email);
                if (existingUser.isPresent()) {
                    user = existingUser.get();
                    // Link social account to existing user
                    createSocialAccount(user, provider, providerId, email, name,
                            providerAccessToken, providerRefreshToken, expiresIn);
                } else {
                    // Create new user
                    user = createNewUserFromSocial(email, name);
                    isNewUser = true;
                    createSocialAccount(user, provider, providerId, email, name,
                            providerAccessToken, providerRefreshToken, expiresIn);
                }
            } else {
                // No email from provider - generate a unique email
                String generatedEmail = provider + "_" + providerId + "@social.mohe.app";
                user = createNewUserFromSocial(generatedEmail, name);
                isNewUser = true;
                createSocialAccount(user, provider, providerId, generatedEmail, name,
                        providerAccessToken, providerRefreshToken, expiresIn);
            }
        }

        // Update last login
        userRepository.updateLastLoginAt(user.getId(), OffsetDateTime.now());

        // Generate JWT tokens
        return generateLoginResponse(user);
    }

    /**
     * Create new user from social login
     */
    private User createNewUserFromSocial(String email, String name) {
        User user = new User();
        user.setEmail(email);
        // Set a random password hash for social users (they can't login with password)
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setNickname(generateUniqueNickname(name));
        user.setIsOnboardingCompleted(false);
        return userRepository.save(user);
    }

    /**
     * Generate unique nickname
     */
    private String generateUniqueNickname(String baseName) {
        String nickname = baseName != null && !baseName.isEmpty() ? baseName : "사용자";
        // Add random suffix if nickname exists
        if (userRepository.existsByNickname(nickname)) {
            nickname = nickname + "_" + new Random().nextInt(10000);
        }
        // Ensure uniqueness
        while (userRepository.existsByNickname(nickname)) {
            nickname = (baseName != null ? baseName : "사용자") + "_" + new Random().nextInt(100000);
        }
        return nickname;
    }

    /**
     * Create social account record
     */
    private void createSocialAccount(User user, String provider, String providerId, String email,
                                     String name, String accessToken, String refreshToken, Object expiresIn) {
        SocialAccount socialAccount = new SocialAccount(user, provider, providerId, email, name);
        socialAccount.setAccessToken(accessToken);
        socialAccount.setRefreshToken(refreshToken);
        if (expiresIn != null) {
            int seconds = expiresIn instanceof Integer ? (Integer) expiresIn : Integer.parseInt(expiresIn.toString());
            socialAccount.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(seconds));
        }
        socialAccountRepository.save(socialAccount);
    }

    /**
     * Generate login response with JWT tokens
     */
    private LoginResponse generateLoginResponse(User user) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities()
        );

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

    /**
     * Exchange Kakao authorization code for tokens
     */
    private Map<String, Object> exchangeKakaoCode(String code, String redirectUri) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        if (kakaoClientSecret != null && !kakaoClientSecret.isEmpty()) {
            params.add("client_secret", kakaoClientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("카카오 인증에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * Get Kakao user info
     */
    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("카카오 사용자 정보를 가져오는데 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * Exchange Google authorization code for tokens
     */
    private Map<String, Object> exchangeGoogleCode(String code, String redirectUri) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("구글 인증에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * Get Google user info
     */
    private Map<String, Object> getGoogleUserInfo(String accessToken) {
        String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("구글 사용자 정보를 가져오는데 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * Get linked social accounts for a user
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getLinkedAccounts(Long userId) {
        List<SocialAccount> accounts = socialAccountRepository.findByUserId(userId);
        List<Map<String, String>> result = new ArrayList<>();

        for (SocialAccount account : accounts) {
            Map<String, String> info = new HashMap<>();
            info.put("provider", account.getProvider());
            info.put("email", account.getProviderEmail());
            info.put("name", account.getProviderName());
            result.add(info);
        }

        return result;
    }

    /**
     * Unlink social account
     */
    public void unlinkSocialAccount(Long userId, String provider) {
        Optional<SocialAccount> account = socialAccountRepository.findByUserIdAndProvider(userId, provider);
        if (account.isEmpty()) {
            throw new RuntimeException("연동된 " + provider + " 계정이 없습니다");
        }

        // Check if user has password set or other social accounts
        User user = account.get().getUser();
        List<SocialAccount> allAccounts = socialAccountRepository.findByUserId(userId);

        // User must have at least one login method
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()
                && !user.getPasswordHash().startsWith("social_");
        boolean hasOtherSocialAccounts = allAccounts.size() > 1;

        if (!hasPassword && !hasOtherSocialAccounts) {
            throw new RuntimeException("비밀번호 설정 후 연동을 해제할 수 있습니다");
        }

        socialAccountRepository.delete(account.get());
    }
}
