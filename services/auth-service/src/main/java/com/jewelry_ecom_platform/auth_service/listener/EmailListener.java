package com.jewelry_ecom_platform.auth_service.listener;

import com.jewelry_ecom_platform.auth_service.dto.EmailVerificationDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailListener {

    private final JavaMailSender mailSender;

    public EmailListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = "email.verification.queue")
    public void sendVerificationEmail(EmailVerificationDto dto) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(dto.getEmail());
        message.setSubject("Verify Your Email");
        message.setText("Click the link to verify: " + dto.getVerificationUrl());
        mailSender.send(message);
        System.out.println("Verification email sent to " + dto.getEmail());
    }
}
