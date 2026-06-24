package com.team01.freelance.proposal.saga;

import com.team01.freelance.proposal.dto.FeignContractDTO;
import com.team01.freelance.proposal.dto.FeignJobDTO;
import com.team01.freelance.proposal.dto.FeignUserDTO;
import com.team01.freelance.proposal.feign.ContractServiceClient;
import com.team01.freelance.proposal.feign.JobServiceClient;
import com.team01.freelance.proposal.feign.UserServiceClient;
import com.team01.freelance.proposal.messaging.publishers.ProposalEventPublisher;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SagaTriggerService {

    private static final Logger log = LoggerFactory.getLogger(SagaTriggerService.class);

    private final ProposalRepository proposalRepository;
    private final ProposalStateMachine stateMachine;
    private final JobServiceClient jobServiceClient;
    private final UserServiceClient userServiceClient;
    private final ContractServiceClient contractServiceClient;
    private final ProposalEventPublisher proposalEventPublisher;

    public SagaTriggerService(
            ProposalRepository proposalRepository,
            ProposalStateMachine stateMachine,
            JobServiceClient jobServiceClient,
            UserServiceClient userServiceClient,
            ContractServiceClient contractServiceClient,
            ProposalEventPublisher proposalEventPublisher) {
        this.proposalRepository = proposalRepository;
        this.stateMachine = stateMachine;
        this.jobServiceClient = jobServiceClient;
        this.userServiceClient = userServiceClient;
        this.contractServiceClient = contractServiceClient;
        this.proposalEventPublisher = proposalEventPublisher;
    }

    @Transactional
    public Proposal triggerCompletion(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + proposalId));

        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            throw new IllegalArgumentException("Only ACCEPTED proposals can be completed");
        }

        FeignContractDTO contract = runPreChecks(proposal);
        ProposalStatus oldStatus = proposal.getStatus();
        stateMachine.transition(proposal, ProposalStatus.COMPLETING);
        Proposal saved = proposalRepository.saveAndFlush(proposal);
        logProposalTransition(proposalId, oldStatus, ProposalStatus.COMPLETING);
        proposalEventPublisher.publishProposalCompleted(saved, contract);
        return saved;
    }

    private FeignContractDTO runPreChecks(Proposal proposal) {
        validateJob(proposal.getJobId());
        validateFreelancer(proposal.getFreelancerId());
        return resolveActiveContract(proposal.getId());
    }

    private void validateJob(Long jobId) {
        try {
            FeignJobDTO job = jobServiceClient.getJob(jobId);
            if (job.getStatus() != null && "CLOSED".equalsIgnoreCase(job.getStatus())) {
                throw new IllegalArgumentException("Job is already CLOSED");
            }
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Job not found");
        } catch (FeignException e) {
            log.warn("job-service unavailable for job {}: {}", jobId, e.getMessage());
            throw new IllegalStateException("Job service temporarily unavailable");
        }
    }

    private void validateFreelancer(Long freelancerId) {
        try {
            FeignUserDTO user = userServiceClient.getUser(freelancerId);
            if (user.getStatus() == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new IllegalArgumentException("Freelancer is not ACTIVE");
            }
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Freelancer not found");
        } catch (FeignException e) {
            log.warn("user-service unavailable for freelancer {}: {}", freelancerId, e.getMessage());
            throw new IllegalStateException("User service temporarily unavailable");
        }
    }

    private FeignContractDTO resolveActiveContract(Long proposalId) {
        try {
            FeignContractDTO contract = contractServiceClient.getActiveContract(proposalId);
            if (contract.getStatus() == null || !"ACTIVE".equalsIgnoreCase(contract.getStatus())) {
                throw new IllegalArgumentException("No ACTIVE contract found for proposal");
            }
            return contract;
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("No ACTIVE contract found for proposal");
        } catch (FeignException e) {
            log.warn("contract-service unavailable for proposal {}: {}", proposalId, e.getMessage());
            throw new IllegalStateException("Contract service temporarily unavailable");
        }
    }

    private void logProposalTransition(Long proposalId, ProposalStatus oldStatus, ProposalStatus newStatus) {
        MDC.put("proposalId", String.valueOf(proposalId));
        try {
            log.info("Proposal {} transitioning {} -> {}", proposalId, oldStatus, newStatus);
        } finally {
            MDC.remove("proposalId");
        }
    }
}
