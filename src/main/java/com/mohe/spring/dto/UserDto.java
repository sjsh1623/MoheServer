package com.mohe.spring.dto;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public class UserDto {

    // User Preferences
    public record UserPreferencesRequest(
        MbtiPreference mbti,
        String ageRange,
        List<String> spacePreferences,
        String transportationMethod
    ) {
        public UserPreferencesRequest(MbtiPreference mbti, String ageRange, List<String> spacePreferences, String transportationMethod) {
            this.mbti = mbti;
            this.ageRange = ageRange;
            this.spacePreferences = spacePreferences != null ? spacePreferences : List.of();
            this.transportationMethod = transportationMethod;
        }
    }

    public record MbtiPreference(
        String extroversion, // E or I
        String sensing,      // S or N
        String thinking,     // T or F
        String judging       // J or P
    ) {}

    public record UserPreferencesResponse(
        UserPreferencesData preferences
    ) {}

    public record UserPreferencesData(
        String mbti,
        String ageRange,
        List<String> spacePreferences,
        String transportationMethod
    ) {}

    // User Profile
    public record UserProfileResponse(
        UserProfileData user
    ) {}

    public record UserProfileData(
        String id,
        String email,
        String nickname,
        String mbti,
        String ageRange,
        List<String> spacePreferences,
        String transportationMethod,
        String profileImage,
        OffsetDateTime createdAt
    ) {
        public UserProfileData(String id, String email, String nickname, String mbti, 
                             String ageRange, List<String> spacePreferences, String transportationMethod, 
                             String profileImage, OffsetDateTime createdAt) {
            this.id = id;
            this.email = email;
            this.nickname = nickname;
            this.mbti = mbti;
            this.ageRange = ageRange;
            this.spacePreferences = spacePreferences != null ? spacePreferences : List.of();
            this.transportationMethod = transportationMethod;
            this.profileImage = profileImage;
            this.createdAt = createdAt;
        }
    }

    // Profile Edit
    public record ProfileEditRequest(
        @Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다")
        String nickname,
        
        String profileImage // base64 image data
    ) {}

    public record ProfileEditResponse(
        ProfileEditData user
    ) {}

    public record ProfileEditData(
        String id,
        String nickname,
        String profileImage
    ) {}

    // Agreements (약관 동의)
    public record AgreementsRequest(
        Boolean terms,     // 이용약관 동의
        Boolean privacy,   // 개인정보 수집 및 이용 동의
        Boolean location,  // 위치 정보 이용약관 동의
        Boolean age14      // 만 14세 이상 확인
    ) {}

    public record AgreementsResponse(
        String message
    ) {}

    // Onboarding Complete (온보딩 완료)
    public record OnboardingCompleteRequest(
        Long userId
    ) {}

    public record OnboardingCompleteResponse(
        String message
    ) {}
}