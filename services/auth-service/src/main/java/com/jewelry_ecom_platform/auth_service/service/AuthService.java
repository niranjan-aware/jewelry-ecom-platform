package com.jewelry_ecom_platform.auth_service.service;

import com.jewelry_ecom_platform.auth_service.dto.LoginRequest;
import com.jewelry_ecom_platform.auth_service.dto.RegisterRequest;
import com.jewelry_ecom_platform.auth_service.repository.UserRepository;
import com.jewelry_ecom_platform.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import com.jewelry_ecom_platform.auth_service.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    public String register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent())
            throw new RuntimeException("Email already exists");

        var user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role("CUSTOMER")
                .emailVerified(false)
                .build();

        userRepository.save(user);
        // TODO: Send email verification link using RabbitMQ later
        return "User registered. Please verify email.";
    }

    public Map<String, String> login(LoginRequest req) {
        var user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid credentials");

        String token = jwtUtil.generateToken(user.getEmail());
        String refresh = jwtUtil.generateRefreshToken(user.getEmail());

        redisTemplate.opsForValue().set(user.getEmail(), refresh);

        return Map.of("accessToken", token, "refreshToken", refresh);
    }

    public void logout(String email) {
        redisTemplate.delete(email);
    }
}

