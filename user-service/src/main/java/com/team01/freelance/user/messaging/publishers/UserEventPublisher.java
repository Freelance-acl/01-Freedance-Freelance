package com.team01.freelance.user.messaging.publishers;

import com.team01.freelance.contracts.events.UserDeactivatedEvent;
import com.team01.freelance.contracts.events.UserRegisteredEvent;
import com.team01.freelance.user.config.RabbitMQConfig;
import com.team01.freelance.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("registered user and user id must not be null");
        }

        String role = user.getRole() == null ? null : user.getRole().name();

        UserRegisteredEvent event = new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                role
        );

        publish(UserRegisteredEvent.ROUTING_KEY, event);
    }

    public void publishUserDeactivated(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("deactivated user id must not be null");
        }

        UserDeactivatedEvent event = new UserDeactivatedEvent(userId);

        publish(UserDeactivatedEvent.ROUTING_KEY, event);
    }

    private void publish(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, routingKey, event);
            log.info("Published event {} to exchange {}", routingKey, RabbitMQConfig.USER_EXCHANGE);
        } catch (AmqpException ex) {
            log.warn("Failed to publish event {} to exchange {}: {}",
                    routingKey,
                    RabbitMQConfig.USER_EXCHANGE,
                    ex.getMessage());
        }
    }
}