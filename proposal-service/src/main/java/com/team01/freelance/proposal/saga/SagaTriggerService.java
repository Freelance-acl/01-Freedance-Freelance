package com.team01.freelance.proposal.saga;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.proposal.dto.FeignContractDTO;
import com.team01.freelance.proposal.dto.FeignJobDTO;
import com.team01.freelance.proposal.dto.FeignUserDTO;
import com.team01.freelance.proposal.feign.ContractServiceClient;
import com.team01.freelance.proposal.feign.JobServiceClient;
import com.team01.freelance.proposal.feign.UserServiceClient;
import com.team01.freelance.proposal.messaging.ProposalEventPublisher;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SagaTriggerService {

    private static final Logger log = LoggerFactory.getLogger(SagaTriggerService.class);

    private final ProposalRepository proposalRepository;
    private final ProposalStateMachine stateMachine;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final DataSource dataSource;

    @Autowired(required = false)
    private JobServiceClient jobServiceClient;

    @Autowired(required = false)
    private UserServiceClient userServiceClient;

    @Autowired(required = false)
    private ContractServiceClient contractServiceClient;

    @Autowired(required = false)
    private ProposalEventPublisher proposalEventPublisher;

    public SagaTriggerService(
            ProposalRepository proposalRepository,
            ProposalStateMachine stateMachine,
            JobRepository jobRepository,
            UserRepository userRepository,
            ContractRepository contractRepository,
            DataSource dataSource) {
        this.proposalRepository = proposalRepository;
        this.stateMachine = stateMachine;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.contractRepository = contractRepository;
        this.dataSource = dataSource;
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
        if (proposalEventPublisher != null) {
            proposalEventPublisher.publishProposalCompleted(saved, contract);
        }
        return saved;
    }

    private FeignContractDTO runPreChecks(Proposal proposal) {
        validateJob(proposal.getJobId());
        validateFreelancer(proposal.getFreelancerId());
        return resolveActiveContract(proposal.getId());
    }

    private void validateJob(Long jobId) {
        if (usesIsolatedDatabase() && jobServiceClient != null) {
            try {
                FeignJobDTO job = jobServiceClient.getJob(jobId);
                if (job.getStatus() != null && "CLOSED".equalsIgnoreCase(job.getStatus())) {
                    throw new IllegalArgumentException("Job is already CLOSED");
                }
                return;
            } catch (FeignException.NotFound e) {
                throw new IllegalArgumentException("Job not found");
            } catch (FeignException e) {
                log.warn("job-service unavailable for job {}: {}", jobId, e.getMessage());
                throw new IllegalStateException("Job service temporarily unavailable");
            }
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new IllegalArgumentException("Job is already CLOSED");
        }
    }

    private void validateFreelancer(Long freelancerId) {
        if (usesIsolatedDatabase() && userServiceClient != null) {
            try {
                FeignUserDTO user = userServiceClient.getUser(freelancerId);
                if (user.getStatus() == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                    throw new IllegalArgumentException("Freelancer is not ACTIVE");
                }
                return;
            } catch (FeignException.NotFound e) {
                throw new IllegalArgumentException("Freelancer not found");
            } catch (FeignException e) {
                log.warn("user-service unavailable for freelancer {}: {}", freelancerId, e.getMessage());
                throw new IllegalStateException("User service temporarily unavailable");
            }
        }

        User user = userRepository.findById(freelancerId)
                .orElseThrow(() -> new IllegalArgumentException("Freelancer not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Freelancer is not ACTIVE");
        }
    }

    private FeignContractDTO resolveActiveContract(Long proposalId) {
        if (usesIsolatedDatabase() && contractServiceClient != null) {
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

        Contract contract = contractRepository.findByProposalId(proposalId)
                .filter(existing -> existing.getStatus() == ContractStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No ACTIVE contract found for proposal"));
        FeignContractDTO dto = new FeignContractDTO();
        dto.setId(contract.getId());
        dto.setJobId(contract.getJobId());
        dto.setFreelancerId(contract.getFreelancerId());
        dto.setProposalId(contract.getProposalId());
        dto.setAgreedAmount(contract.getAgreedAmount());
        dto.setStatus(contract.getStatus().name());
        return dto;
    }

    private boolean usesIsolatedDatabase() {
        try (var connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (Exception ex) {
            return false;
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
