package com.team01.freelance.wallet.service;

import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutMethod;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.observer.EntityObserver;
import com.team01.freelance.wallet.repository.PayoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceObserverTest {

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private EntityObserver mockObserver;

    @InjectMocks
    private PayoutService payoutService;

    private Payout validPayout;

    @BeforeEach
    void setup() {
        validPayout = new Payout();
        validPayout.setId(99L);
        validPayout.setStatus(PayoutStatus.COMPLETED);
        validPayout.setAmount(150.0);
        validPayout.setMethod(PayoutMethod.PAYPAL);
    }

    @Test
    void testRefundPayout_TriggersObserverWithCorrectPayload() {
        when(payoutRepository.findById(99L)).thenReturn(Optional.of(validPayout));
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        payoutService.registerObserver(mockObserver);

        payoutService.refundPayout(99L, "User requested refund");

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(mockObserver, times(1)).onEvent(actionCaptor.capture(), payloadCaptor.capture());

        assertEquals("REFUNDED", actionCaptor.getValue());
        assertTrue(payloadCaptor.getValue() instanceof Map, "Payload must be a Map for MongoEventLogger");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();

        assertEquals(99L, payload.get("payoutId"));
        assertEquals(150.0, payload.get("amount"));
        assertEquals("PAYPAL", payload.get("method"));
    }

    @Test
    void testRefundPayout_UnregisteredObserver_ReceivesNoEvent() {
        when(payoutRepository.findById(99L)).thenReturn(Optional.of(validPayout));
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        payoutService.registerObserver(mockObserver);
        payoutService.unregisterObserver(mockObserver);

        payoutService.refundPayout(99L, "Fraudulent charge");

        verify(mockObserver, never()).onEvent(anyString(), any());
    }

    @Test
    void testRefundPayout_NotCompleted_ThrowsExceptionAndNoEventFired() {
        validPayout.setStatus(PayoutStatus.PENDING);
        when(payoutRepository.findById(99L)).thenReturn(Optional.of(validPayout));

        payoutService.registerObserver(mockObserver);

        assertThrows(ResponseStatusException.class, () -> {
            payoutService.refundPayout(99L, "Trying to refund pending");
        });

        verify(mockObserver, never()).onEvent(anyString(), any());
    }
}