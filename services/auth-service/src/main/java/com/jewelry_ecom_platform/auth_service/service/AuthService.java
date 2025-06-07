package com.jewelry_ecom_platform.auth_service.service;

import com.jewelry_ecom_platform.auth_service.dto.*;

public interface AuthService {
    ApiResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    ApiResponse logout(String token);
    ApiResponse verifyEmail(String token);
    ApiResponse forgotPassword(ForgotPasswordRequest request);
    ApiResponse resetPassword(ResetPasswordRequest request);
    ApiResponse validateToken(String token);
    AuthResponse refreshToken(String refreshToken);
}