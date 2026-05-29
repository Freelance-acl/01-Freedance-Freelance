package com.team01.freelance.wallet.strategy;

import com.team01.freelance.wallet.dto.MilestoneReversalRequest;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.model.PayoutMethod;
import com.team01.freelance.wallet.model.PayoutStatus;
import com.team01.freelance.wallet.repository.PayoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundStrategySelectorTest {

    @Mock
    private PayoutRepository payoutRepository;

    private RefundStrategySelector selector;

    @BeforeEach
    void setUp() {
        selector = new RefundStrategySelector(
                new FullPayoutReversalStrategy(),
                new MilestoneReversalStrategy(payoutRepository),
                new NoReversalStrategy());
    }

    @Test
    void select_withinWindow_fullScope_returnsFullStrategy() {
        Payout payout = recentPayout();
        MilestoneReversalRequest request = new MilestoneReversalRequest();
        request.setReversalScope("FULL");

        assertInstanceOf(FullPayoutReversalStrategy.class, selector.select(payout, request));
    }

    @Test
    void select_withinWindow_defaultScope_returnsMilestoneStrategy() {
        Payout payout = recentPayout();
        MilestoneReversalRequest request = new MilestoneReversalRequest();
        request.setReversalScope("MILESTONE_ONLY");

        assertInstanceOf(MilestoneReversalStrategy.class, selector.select(payout, request));
    }

    @Test
    void select_expiredWindow_returnsNoReversalWithoutCalculatingAmount() {
        Payout payout = recentPayout();
        payout.setCreatedAt(LocalDateTime.now().minusDays(40));
        MilestoneReversalRequest request = new MilestoneReversalRequest();
        request.setReversalScope("MILESTONE_ONLY");

        assertInstanceOf(NoReversalStrategy.class, selector.select(payout, request));
    }

    @Test
    void select_expiredWindow_returnsNoReversalStrategy() {
        Payout payout = recentPayout();
        payout.setCreatedAt(LocalDateTime.now().minusDays(31));
        MilestoneReversalRequest request = new MilestoneReversalRequest();
        request.setReversalScope("FULL");

        assertInstanceOf(NoReversalStrategy.class, selector.select(payout, request));
    }

    @Test
    void milestoneStrategy_sumsIncompleteMilestones() {
        Payout payout = recentPayout();
        when(payoutRepository.sumIncompleteMilestoneAmounts(payout.getContractId())).thenReturn(250.0);

        MilestoneReversalRequest request = new MilestoneReversalRequest();
        RefundResult result = new MilestoneReversalStrategy(payoutRepository)
                .calculateRefund(payout, request);

        assertEquals(250.0, result.amount());
        assertEquals("MilestoneReversalStrategy", result.strategyName());
    }

    private static Payout recentPayout() {
        Payout payout = new Payout();
        payout.setId(1L);
        payout.setContractId(10L);
        payout.setAmount(500.0);
        payout.setMethod(PayoutMethod.BANK_TRANSFER);
        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setCreatedAt(LocalDateTime.now().minusDays(3));
        return payout;
    }
}
