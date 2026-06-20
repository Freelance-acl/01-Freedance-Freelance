package com.team01.freelance.user.messaging;

import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.user.config.RabbitMQConfig;
import com.team01.freelance.user.model.FreelancerProposalStat;
import com.team01.freelance.user.repository.FreelancerProposalStatRepository;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RabbitListener(queues = RabbitMQConfig.QUEUE)
public class ProposalSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProposalSagaConsumer.class);

    private final UserRepository userRepository;
    private final FreelancerProposalStatRepository statRepository;

    public ProposalSagaConsumer(
            UserRepository userRepository,
            FreelancerProposalStatRepository statRepository) {
        this.userRepository = userRepository;
        this.statRepository = statRepository;
    }

    @RabbitHandler
    @Transactional
    public void handleProposalCompleted(ProposalCompletedEvent event) {
        validateCompletedEvent(event);

        FreelancerProposalStat stat = statRepository.findById(event.proposalId())
                .orElseGet(FreelancerProposalStat::new);

        if (stat.isCounted()) {
            log.info("Ignoring duplicate proposal.completed event for proposalId={}", event.proposalId());
            return;
        }

        BigDecimal amount = normalizeAmount(event.agreedAmount());

        stat.setProposalId(event.proposalId());
        stat.setJobId(event.jobId());
        stat.setFreelancerId(event.freelancerId());
        stat.setContractId(event.contractId());
        stat.setAgreedAmount(amount);
        stat.setCounted(true);
        stat.setCompletedAt(LocalDateTime.now());
        stat.setCancelledAt(null);
        stat.setCancellationReason(null);

        int updated = userRepository.incrementFreelancerStats(event.freelancerId(), amount);
        if (updated == 0) {
            throw new EntityNotFoundException("Freelancer not found with id: " + event.freelancerId());
        }

        statRepository.save(stat);

        log.info("Updated freelancer stats for proposal.completed proposalId={}, freelancerId={}, amount={}",
                event.proposalId(),
                event.freelancerId(),
                amount);
    }

    @RabbitHandler
    @Transactional
    public void handleProposalCancelled(ProposalCancelledEvent event) {
        validateCancelledEvent(event);

        FreelancerProposalStat stat = statRepository.findById(event.proposalId())
                .orElseGet(FreelancerProposalStat::new);

        if (!stat.isCounted()) {
            stat.setProposalId(event.proposalId());
            stat.setJobId(event.jobId());
            stat.setFreelancerId(event.freelancerId());
            stat.setCounted(false);
            stat.setCancelledAt(LocalDateTime.now());
            stat.setCancellationReason(event.reason());
            statRepository.save(stat);

            log.info("Stored proposal.cancelled event without stats reversal for proposalId={}", event.proposalId());
            return;
        }

        BigDecimal amount = normalizeAmount(stat.getAgreedAmount());

        int updated = userRepository.decrementFreelancerStats(stat.getFreelancerId(), amount);
        if (updated == 0) {
            throw new EntityNotFoundException("Freelancer not found with id: " + stat.getFreelancerId());
        }

        stat.setCounted(false);
        stat.setCancelledAt(LocalDateTime.now());
        stat.setCancellationReason(event.reason());
        statRepository.save(stat);

        log.info("Reversed freelancer stats for proposal.cancelled proposalId={}, freelancerId={}, amount={}",
                event.proposalId(),
                stat.getFreelancerId(),
                amount);
    }

    @RabbitHandler(isDefault = true)
    public void handleUnknown(Object event) {
        log.warn("Ignoring unsupported event on queue {}: {}", RabbitMQConfig.QUEUE, event);
    }

    private void validateCompletedEvent(ProposalCompletedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("proposal.completed event must not be null");
        }
        if (event.proposalId() == null) {
            throw new IllegalArgumentException("proposal.completed event is missing proposalId");
        }
        if (event.freelancerId() == null) {
            throw new IllegalArgumentException("proposal.completed event is missing freelancerId");
        }
        if (event.agreedAmount() == null) {
            throw new IllegalArgumentException("proposal.completed event is missing agreedAmount");
        }
    }

    private void validateCancelledEvent(ProposalCancelledEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("proposal.cancelled event must not be null");
        }
        if (event.proposalId() == null) {
            throw new IllegalArgumentException("proposal.cancelled event is missing proposalId");
        }
        if (event.freelancerId() == null) {
            throw new IllegalArgumentException("proposal.cancelled event is missing freelancerId");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
