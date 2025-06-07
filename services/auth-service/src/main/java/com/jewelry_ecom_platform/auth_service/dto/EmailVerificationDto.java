package com.jewelry_ecom_platform.auth_service.dto;

import lombok.Data;

@Data
public class EmailVerificationDto {
    private String email;
    private String verificationToken;
    private String verificationLink;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getVerificationLink() {
        return verificationLink;
    }

    public void setVerificationLink(String verificationLink) {
        this.verificationLink = verificationLink;
    }

    public String getVerificationUrl() {
        return  verificationLink;
    }
}