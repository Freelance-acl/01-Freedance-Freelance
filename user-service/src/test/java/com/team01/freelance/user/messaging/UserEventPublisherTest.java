package com.team01.freelance.user.messaging;

import com.team01.freelance.contracts.events.UserDeactivatedEvent;
import com.team01.freelance.contracts.events.UserRegisteredEvent;
import com.team01.freelance.user.config.RabbitMQConfig;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final UserEventPublisher publisher = new UserEventPublisher(rabbitTemplate);

    @Test
    void publishUserRegisteredSendsEventToUserExchange() {
        User user = new User();
        user.setId(42L);
        user.setEmail("freelancer@example.com");
        user.setRole(UserRole.FREELANCER);

        publisher.publishUserRegistered(user);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.USER_EXCHANGE),
                eq(UserRegisteredEvent.ROUTING_KEY),
                eventCaptor.capture()
        );

        UserRegisteredEvent event = eventCaptor.getValue();

        assertEquals(42L, event.userId());
        assertEquals("freelancer@example.com", event.email());
        assertEquals("FREELANCER", event.role());
    }

    @Test
    void publishUserDeactivatedSendsEventToUserExchange() {
        publisher.publishUserDeactivated(42L);

        ArgumentCaptor<UserDeactivatedEvent> eventCaptor = ArgumentCaptor.forClass(UserDeactivatedEvent.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.USER_EXCHANGE),
                eq(UserDeactivatedEvent.ROUTING_KEY),
                eventCaptor.capture()
        );

        assertEquals(42L, eventCaptor.getValue().userId());
    }

    @Test
    void publishUserRegisteredRejectsNullUser() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publishUserRegistered(null));
    }

    @Test
    void publishUserDeactivatedRejectsNullUserId() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publishUserDeactivated(null));
    }

    @Test
    void publisherDoesNotCrashWhenRabbitMqIsUnavailable() {
        User user = new User();
        user.setId(42L);
        user.setEmail("freelancer@example.com");
        user.setRole(UserRole.FREELANCER);

        doThrow(new AmqpException("RabbitMQ unavailable"))
                .when(rabbitTemplate)
                .convertAndSend(
                        eq(RabbitMQConfig.USER_EXCHANGE),
                        eq(UserRegisteredEvent.ROUTING_KEY),
                        any(UserRegisteredEvent.class)
                );

        publisher.publishUserRegistered(user);
    }
}