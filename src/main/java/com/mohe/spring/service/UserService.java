package com.mohe.spring.service;

import com.mohe.spring.dto.*;
import com.mohe.spring.entity.TermsAgreement;
import com.mohe.spring.entity.User;
import com.mohe.spring.repository.UserRepository;
import com.mohe.spring.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;

@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public UserPreferencesResponse updatePreferences(UserPreferencesRequest request) {
        User currentUser = getCurrentUser();
        
        String mbtiString = null;
        if (request.getMbti() != null) {
            MbtiPreference mbti = request.getMbti();
            mbtiString = mbti.getExtroversion() + mbti.getSensing() + mbti.getThinking() + mbti.getJudging();
        }
        
        currentUser.setMbti(mbtiString != null ? mbtiString : currentUser.getMbti());
        currentUser.setAgeRange(request.getAgeRange() != null ? request.getAgeRange() : currentUser.getAgeRange());
        currentUser.setTransportation(request.getTransportationMethod() != null ? request.getTransportationMethod() : currentUser.getTransportation());
        currentUser.setIsOnboardingCompleted(true);
        
        userRepository.save(currentUser);
        
        // TODO: Save space preferences to preferences table
        
        UserPreferencesData preferencesData = new UserPreferencesData();
        preferencesData.setMbti(currentUser.getMbti());
        preferencesData.setAgeRange(currentUser.getAgeRange());
        preferencesData.setSpacePreferences(request.getSpacePreferences());
        preferencesData.setTransportationMethod(currentUser.getTransportation());
        
        UserPreferencesResponse response = new UserPreferencesResponse();
        response.setPreferences(preferencesData);
        
        return response;
    }
    
    public UserProfileResponse getUserProfile() {
        User currentUser = getCurrentUser();
        
        // TODO: Fetch space preferences from preferences table
        // For now, return empty list
        
        UserProfileData profileData = new UserProfileData();
        profileData.setId(currentUser.getId().toString());
        profileData.setEmail(currentUser.getEmail());
        profileData.setNickname(currentUser.getNickname());
        profileData.setMbti(currentUser.getMbti());
        profileData.setAgeRange(currentUser.getAgeRange());
        profileData.setSpacePreferences(Collections.emptyList());
        profileData.setTransportationMethod(currentUser.getTransportation());
        profileData.setProfileImage(currentUser.getProfileImageUrl());
        profileData.setCreatedAt(currentUser.getCreatedAt());
        
        UserProfileResponse response = new UserProfileResponse();
        response.setUser(profileData);
        
        return response;
    }
    
    public ProfileEditResponse editProfile(ProfileEditRequest request) {
        User currentUser = getCurrentUser();
        
        // Check nickname uniqueness if changed
        if (request.getNickname() != null && !request.getNickname().equals(currentUser.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new RuntimeException("이미 사용중인 닉네임입니다");
            }
        }
        
        currentUser.setNickname(request.getNickname() != null ? request.getNickname() : currentUser.getNickname());
        currentUser.setProfileImageUrl(request.getProfileImage() != null ? request.getProfileImage() : currentUser.getProfileImageUrl());
        
        User savedUser = userRepository.save(currentUser);
        
        ProfileEditData editData = new ProfileEditData();
        editData.setId(savedUser.getId().toString());
        editData.setNickname(savedUser.getNickname());
        editData.setProfileImage(savedUser.getProfileImageUrl());
        
        ProfileEditResponse response = new ProfileEditResponse();
        response.setUser(editData);
        
        return response;
    }
    
    public UserDto.AgreementsResponse saveAgreements(UserDto.AgreementsRequest request) {
        User currentUser = getCurrentUser();

        // Clear existing agreements
        currentUser.getTermsAgreements().clear();

        // Save new agreements
        if (Boolean.TRUE.equals(request.terms())) {
            TermsAgreement termsAgreement = new TermsAgreement();
            termsAgreement.setUser(currentUser);
            termsAgreement.setTermsCode("service-terms");
            termsAgreement.setAgreed(true);
            termsAgreement.setAgreedAt(OffsetDateTime.now());
            currentUser.getTermsAgreements().add(termsAgreement);
        }

        if (Boolean.TRUE.equals(request.privacy())) {
            TermsAgreement privacyAgreement = new TermsAgreement();
            privacyAgreement.setUser(currentUser);
            privacyAgreement.setTermsCode("privacy-policy");
            privacyAgreement.setAgreed(true);
            privacyAgreement.setAgreedAt(OffsetDateTime.now());
            currentUser.getTermsAgreements().add(privacyAgreement);
        }

        if (Boolean.TRUE.equals(request.location())) {
            TermsAgreement locationAgreement = new TermsAgreement();
            locationAgreement.setUser(currentUser);
            locationAgreement.setTermsCode("location-terms");
            locationAgreement.setAgreed(true);
            locationAgreement.setAgreedAt(OffsetDateTime.now());
            currentUser.getTermsAgreements().add(locationAgreement);
        }

        if (Boolean.TRUE.equals(request.age14())) {
            TermsAgreement ageAgreement = new TermsAgreement();
            ageAgreement.setUser(currentUser);
            ageAgreement.setTermsCode("age-verification");
            ageAgreement.setAgreed(true);
            ageAgreement.setAgreedAt(OffsetDateTime.now());
            currentUser.getTermsAgreements().add(ageAgreement);
        }

        userRepository.save(currentUser);

        return new UserDto.AgreementsResponse("약관 동의 완료");
    }

    public UserDto.OnboardingCompleteResponse completeOnboarding(UserDto.OnboardingCompleteRequest request) {
        User currentUser = getCurrentUser();
        currentUser.setIsOnboardingCompleted(true);
        userRepository.save(currentUser);

        return new UserDto.OnboardingCompleteResponse("온보딩 완료");
    }

    private User getCurrentUser() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }
}