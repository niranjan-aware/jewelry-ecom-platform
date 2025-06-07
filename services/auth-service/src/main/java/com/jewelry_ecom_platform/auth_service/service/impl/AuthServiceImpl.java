package com.jewelry_ecom_platform.auth_service.service.impl;

import com.jewelry_ecom_platform.auth_service.dto.*;
import com.jewelry_ecom_platform.auth_service.entity.User;
import com.jewelry_ecom_platform.auth_service.entity.UserSession;
import com.jewelry_ecom_platform.auth_service.exception.AuthException;
import com.jewelry_ecom_platform.auth_service.repository.UserRepository;
import com.jewelry_ecom_platform.auth_service.repository.UserSessionRepository;
import com.jewelry_ecom_platform.auth_service.service.AuthService;
import com.jewelry_ecom_platform.auth_service.service.EmailService;
import com.jewelry_ecom_platform.auth_service.util.JwtUtil;
import com.jewelry_ecom_platform.auth_service.util.TokenGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenGenerator tokenGenerator;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthServiceImpl(UserRepository userRepository, UserSessionRepository sessionRepository,
                           PasswordEncoder passwordEncoder, JwtUtil jwtUtil, TokenGenerator tokenGenerator,
                           EmailService emailService, RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenGenerator = tokenGenerator;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ApiResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered");
        }

        String verificationToken = tokenGenerator.generateVerificationToken();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : "CUSTOMER")
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .build();

        userRepository.save(user);

        EmailVerificationDto emailDto = new EmailVerificationDto();
        emailDto.setEmail(user.getEmail());
        emailDto.setVerificationToken(verificationToken);
        emailDto.setVerificationLink(frontendUrl + "/verify-email?token=" + verificationToken);

        emailService.sendVerificationEmail(emailDto);

        return new ApiResponse(true, "Registration successful. Please check your email for verification.");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new AuthException("Please verify your email before logging in");
        }

        sessionRepository.deactivateAllSessionsByEmail(user.getEmail());

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        UserSession session = UserSession.builder()
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusNanos(jwtUtil.getExpirationTime() * 1_000_000))
                .build();

        sessionRepository.save(session);

        redisTemplate.opsForValue().set(
                "session:" + user.getEmail(),
                accessToken,
                jwtUtil.getExpirationTime(),
                TimeUnit.MILLISECONDS
        );

        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getRole(), jwtUtil.getExpirationTime());
    }

    @Override
    public ApiResponse logout(String token) {
        sessionRepository.deactivateSessionByToken(token);

        String username = jwtUtil.extractUsername(token);
        redisTemplate.delete("session:" + username);

        return new ApiResponse(true, "Logged out successfully");
    }

    @Override
    public ApiResponse verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new AuthException("Invalid verification token"));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AuthException("Verification token expired");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        return new ApiResponse(true, "Email verified successfully");
    }

    @Override
    public ApiResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User not found"));

        String resetToken = tokenGenerator.generateResetToken();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setEmail(user.getEmail());
        resetDto.setResetToken(resetToken);
        resetDto.setResetLink(frontendUrl + "/reset-password?token=" + resetToken);

        emailService.sendPasswordResetEmail(resetDto);

        return new ApiResponse(true, "Password reset link sent to your email");
    }

    @Override
    public ApiResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new AuthException("Invalid reset token"));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AuthException("Reset token expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        sessionRepository.deactivateAllSessionsByEmail(user.getEmail());
        redisTemplate.delete("session:" + user.getEmail());

        return new ApiResponse(true, "Password reset successfully");
    }

    @Override
    public ApiResponse validateToken(String token) {
        if (!jwtUtil.validate(token)) {
            return new ApiResponse(false, "Invalid token");
        }

        String username = jwtUtil.extractUsername(token);
        String cachedToken = redisTemplate.opsForValue().get("session:" + username);

        if (cachedToken == null || !cachedToken.equals(token)) {
            return new ApiResponse(false, "Session expired");
        }

        return new ApiResponse(true, "Token valid", username);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validate(refreshToken)) {
            throw new AuthException("Invalid refresh token");
        }

        UserSession session = sessionRepository.findByRefreshTokenAndActiveTrue(refreshToken)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        User user = userRepository.findByEmail(session.getEmail())
                .orElseThrow(() -> new AuthException("User not found"));

        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        session.setAccessToken(newAccessToken);
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(LocalDateTime.now().plusNanos(jwtUtil.getExpirationTime() * 1_000_000));
        sessionRepository.save(session);

        redisTemplate.opsForValue().set(
                "session:" + user.getEmail(),
                newAccessToken,
                jwtUtil.getExpirationTime(),
                TimeUnit.MILLISECONDS
        );

        return new AuthResponse(newAccessToken, newRefreshToken, user.getEmail(), user.getRole(), jwtUtil.getExpirationTime());
    }
}
