package com.team01.freelance.job.messaging.publishers;

import com.team01.freelance.contracts.events.JobClosedEvent;
import com.team01.freelance.contracts.events.JobRatedEvent;
import com.team01.freelance.contracts.events.JobStatusChangedEvent;
import com.team01.freelance.job.config.RabbitMQConfig;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public JobEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishJobStatusChanged(Long jobId, JobStatus oldStatus, JobStatus newStatus) {
        JobStatusChangedEvent event = new JobStatusChangedEvent(
                jobId,
                oldStatus == null ? null : oldStatus.name(),
                newStatus == null ? null : newStatus.name()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.JOB_EXCHANGE,
                JobStatusChangedEvent.ROUTING_KEY,
                event
        );
    }

    public void publishJobRated(Long jobId, Long contractId, Double rating, Long ratedBy) {
        JobRatedEvent event = new JobRatedEvent(jobId, contractId, rating, ratedBy);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.JOB_EXCHANGE,
                JobRatedEvent.ROUTING_KEY,
                event
        );
    }

    public void publishJobClosed(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("job must not be null");
        }
        JobClosedEvent event = new JobClosedEvent(job.getId(), job.getClientId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.JOB_EXCHANGE,
                JobClosedEvent.ROUTING_KEY,
                event
        );
    }
}
