package com.team01.freelance.contract.messaging;

import com.team01.freelance.contract.config.RabbitMQConfig;
import com.team01.freelance.contract.feign.JobServiceClient;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.contract.support.AbstractIntegrationTest;
import com.team01.freelance.contracts.events.ContractCreatedEvent;
import com.team01.freelance.contracts.events.ContractStatusChangedEvent;
import com.team01.freelance.contracts.events.ProposalAcceptedEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.job.model.Job;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

/**
 * [§8.6 Scenario A] Happy-path saga test for proposal acceptance through contract completion.
 */
@Transactional
class ScenarioAHappyPathSagaIntegrationTest extends AbstractIntegrationTest {

    private static final Long PROPOSAL_ID = 88001L;
    private static final Long JOB_ID = 99001L;
    private static final Long CLIENT_ID = 77001L;
    private static final Long FREELANCER_ID = 66001L;
    private static final BigDecimal AGREED_AMOUNT = new BigDecimal("4500.00");

    @Autowired
    private ContractSagaConsumer contractSagaConsumer;

    @Autowired
    private ContractRepository contractRepository;

    @MockitoBean
    private JobServiceClient jobServiceClient;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void scenarioA_acceptProposalThenCompleteContract_persistsAndPublishesHappyPathEvents() {
        when(jobServiceClient.getJob(JOB_ID)).thenReturn(job(JOB_ID, CLIENT_ID));

        contractSagaConsumer.handle(new ProposalAcceptedEvent(
                PROPOSAL_ID,
                JOB_ID,
                FREELANCER_ID,
                AGREED_AMOUNT));

        Contract activeContract = contractRepository.findByProposalId(PROPOSAL_ID).orElseThrow();
        assertNotNull(activeContract.getId());
        assertEquals(JOB_ID, activeContract.getJobId());
        assertEquals(CLIENT_ID, activeContract.getClientId());
        assertEquals(FREELANCER_ID, activeContract.getFreelancerId());
        assertEquals(AGREED_AMOUNT.doubleValue(), activeContract.getAgreedAmount());
        assertEquals(ContractStatus.ACTIVE, activeContract.getStatus());
        assertNotNull(activeContract.getStartDate());

        ArgumentCaptor<Object> createdEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.CONTRACT_EXCHANGE),
                eq(ContractCreatedEvent.ROUTING_KEY),
                createdEventCaptor.capture());
        ContractCreatedEvent createdEvent = (ContractCreatedEvent) createdEventCaptor.getValue();
        assertEquals(activeContract.getId(), createdEvent.contractId());
        assertEquals(PROPOSAL_ID, createdEvent.proposalId());
        assertEquals(JOB_ID, createdEvent.jobId());
        assertEquals(FREELANCER_ID, createdEvent.freelancerId());
        assertEquals(0, AGREED_AMOUNT.compareTo(createdEvent.agreedAmount()));

        contractSagaConsumer.handle(new ProposalCompletedEvent(
                PROPOSAL_ID,
                JOB_ID,
                FREELANCER_ID,
                activeContract.getId(),
                AGREED_AMOUNT));

        Contract completedContract = contractRepository.findById(activeContract.getId()).orElseThrow();
        assertEquals(ContractStatus.COMPLETED, completedContract.getStatus());
        assertNotNull(completedContract.getEndDate());

        ArgumentCaptor<Object> statusEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(2)).convertAndSend(
                eq(RabbitMQConfig.CONTRACT_EXCHANGE),
                org.mockito.ArgumentMatchers.anyString(),
                statusEventCaptor.capture());

        Optional<ContractStatusChangedEvent> statusChanged = statusEventCaptor.getAllValues().stream()
                .filter(ContractStatusChangedEvent.class::isInstance)
                .map(ContractStatusChangedEvent.class::cast)
                .findFirst();

        ContractStatusChangedEvent statusChangedEvent = statusChanged.orElseThrow();
        assertEquals(activeContract.getId(), statusChangedEvent.contractId());
        assertEquals("ACTIVE", statusChangedEvent.oldStatus());
        assertEquals("COMPLETED", statusChangedEvent.newStatus());
    }

    private static Job job(Long jobId, Long clientId) {
        Job job = new Job();
        job.setId(jobId);
        job.setClientId(clientId);
        job.setTitle("Scenario A Job");
        return job;
    }
}
