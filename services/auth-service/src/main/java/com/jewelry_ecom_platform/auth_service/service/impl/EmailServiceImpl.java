package com.jewelry_ecom_platform.auth_service.service.impl;

import com.jewelry_ecom_platform.auth_service.dto.EmailVerificationDto;
import com.jewelry_ecom_platform.auth_service.dto.PasswordResetDto;
import com.jewelry_ecom_platform.auth_service.service.EmailService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.email}")
    private String emailExchange;

    @Value("${rabbitmq.routing.verification}")
    private String verificationRoutingKey;

    @Value("${rabbitmq.routing.reset}")
    private String resetRoutingKey;

    public EmailServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendVerificationEmail(EmailVerificationDto emailDto) {
        rabbitTemplate.convertAndSend(emailExchange, verificationRoutingKey, emailDto);
    }

    @Override
    public void sendPasswordResetEmail(PasswordResetDto resetDto) {
        rabbitTemplate.convertAndSend(emailExchange, resetRoutingKey, resetDto);
    }
}
