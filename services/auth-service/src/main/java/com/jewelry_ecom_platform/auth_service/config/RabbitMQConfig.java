package com.jewelry_ecom_platform.auth_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.email}")
    private String emailExchange;

    @Value("${rabbitmq.queue.verification}")
    private String verificationQueue;

    @Value("${rabbitmq.queue.reset}")
    private String resetQueue;

    @Value("${rabbitmq.routing.verification}")
    private String verificationRoutingKey;

    @Value("${rabbitmq.routing.reset}")
    private String resetRoutingKey;

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(emailExchange);
    }

    @Bean
    public Queue verificationQueue() {
        return QueueBuilder.durable(verificationQueue).build();
    }

    @Bean
    public Queue resetQueue() {
        return QueueBuilder.durable(resetQueue).build();
    }

    @Bean
    public Binding verificationBinding() {
        return BindingBuilder
                .bind(verificationQueue())
                .to(emailExchange())
                .with(verificationRoutingKey);
    }

    @Bean
    public Binding resetBinding() {
        return BindingBuilder
                .bind(resetQueue())
                .to(emailExchange())
                .with(resetRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
