package com.team01.freelance.contract.config;

import com.team01.freelance.contracts.events.ProposalAcceptedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class SagaMessageConversionTest {
    @Test void objectListenerDispatchesTheSharedEventTypeInsteadOfAMap() {
        var converter = new RabbitMQConfig().jsonMessageConverter();
        var event = new ProposalAcceptedEvent(1L, 2L, 3L, new BigDecimal("900"));
        var message = converter.toMessage(event, new MessageProperties());
        message.getMessageProperties().setInferredArgumentType(Object.class);
        assertEquals(event, assertInstanceOf(ProposalAcceptedEvent.class, converter.fromMessage(message)));
        var service = org.mockito.Mockito.mock(com.team01.freelance.contract.service.ContractService.class);
        var consumer = new com.team01.freelance.contract.messaging.consumers.ContractSagaConsumer(service);
        org.springframework.test.util.ReflectionTestUtils.setField(consumer, "messageConverter", converter);
        consumer.handleMessage(message);
        org.mockito.Mockito.verify(service).handleProposalAccepted(event);
    }
}
