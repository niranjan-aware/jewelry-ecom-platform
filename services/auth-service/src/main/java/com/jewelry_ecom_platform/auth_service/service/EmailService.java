package com.jewelry_ecom_platform.auth_service.service;

import com.jewelry_ecom_platform.auth_service.dto.EmailVerificationDto;
import com.jewelry_ecom_platform.auth_service.dto.PasswordResetDto;

public interface EmailService {
    void sendVerificationEmail(EmailVerificationDto emailDto);
    void sendPasswordResetEmail(PasswordResetDto resetDto);
}