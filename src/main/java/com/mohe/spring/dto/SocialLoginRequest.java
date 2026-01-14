package com.mohe.spring.dto;

import jakarta.validation.constraints.NotBlank;

public class SocialLoginRequest {

    @NotBlank(message = "인증 코드는 필수입니다")
    private String code;

    @NotBlank(message = "리다이렉트 URI는 필수입니다")
    private String redirectUri;

    public SocialLoginRequest() {}

    public SocialLoginRequest(String code, String redirectUri) {
        this.code = code;
        this.redirectUri = redirectUri;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
