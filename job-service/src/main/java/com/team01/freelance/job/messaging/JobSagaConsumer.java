package com.team01.freelance.job.messaging;

import com.team01.freelance.contracts.events.ProposalAcceptedEvent;
import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.contracts.events.ProposalWithdrawnEvent;
import com.team01.freelance.job.config.RabbitMQConfig;
import com.team01.freelance.job.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class JobSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobSagaConsumer.class);

    private final JobService jobService;

    public JobSagaConsumer(JobService jobService) {
        this.jobService = jobService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handle(Object event) {
        try {
            if (event instanceof ProposalAcceptedEvent proposalAccepted) {
                jobService.handleProposalAccepted(proposalAccepted);
            } else if (event instanceof ProposalCompletedEvent proposalCompleted) {
                jobService.handleProposalCompleted(proposalCompleted);
            } else if (event instanceof ProposalCancelledEvent proposalCancelled) {
                jobService.handleProposalCancelled(proposalCancelled);
            } else if (event instanceof ProposalWithdrawnEvent proposalWithdrawn) {
                jobService.handleProposalWithdrawn(proposalWithdrawn);
            } else {
                log.debug("Ignoring unsupported job-service saga event {}", event.getClass().getName());
            }
        } catch (RuntimeException e) {
            log.error("Failed to process job-service saga event: {}", e.getMessage());
            throw e;
        }
    }
}
