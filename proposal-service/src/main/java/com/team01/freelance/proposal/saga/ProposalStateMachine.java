package com.team01.freelance.proposal.saga;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProposalStateMachine {

    private static final Map<ProposalStatus, Set<ProposalStatus>> ALLOWED = Map.of(
            ProposalStatus.ACCEPTED, EnumSet.of(ProposalStatus.COMPLETING),
            ProposalStatus.COMPLETING, EnumSet.of(ProposalStatus.PAYMENT_PENDING),
            ProposalStatus.PAYMENT_PENDING, EnumSet.of(ProposalStatus.PAID, ProposalStatus.PAYMENT_FAILED),
            ProposalStatus.PAYMENT_FAILED, EnumSet.of(ProposalStatus.REFUNDED)
    );

    public boolean canTransition(ProposalStatus from, ProposalStatus to) {
        if (from == to) {
            return true;
        }
        Set<ProposalStatus> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }

    public void transition(Proposal proposal, ProposalStatus target) {
        ProposalStatus current = proposal.getStatus();
        if (current == target) {
            return;
        }
        if (!canTransition(current, target)) {
            throw new IllegalStateException(
                    "Invalid proposal status transition from " + current + " to " + target);
        }
        proposal.setStatus(target);
        if (target == ProposalStatus.PAYMENT_PENDING && proposal.getPaymentPendingAt() == null) {
            proposal.setPaymentPendingAt(LocalDateTime.now());
        }
    }
}
