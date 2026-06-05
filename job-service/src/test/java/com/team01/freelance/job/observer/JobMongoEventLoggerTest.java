package com.team01.freelance.job.observer;

import com.team01.freelance.job.event.JobEvent;
import com.team01.freelance.job.repository.JobEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JobMongoEventLoggerTest {

    @Test
    void onEvent_savesJobClosedEventToMongoRepository() {
        JobEventRepository repository = mock(JobEventRepository.class);
        JobMongoEventLogger logger = new JobMongoEventLogger(repository);

        logger.onEvent("JOB_CLOSED", Map.of(
                "jobId", 1L,
                "status", "CLOSED",
                "source", "S2-F4"
        ));

        ArgumentCaptor<JobEvent> captor = ArgumentCaptor.forClass(JobEvent.class);
        verify(repository).save(captor.capture());

        JobEvent event = captor.getValue();
        assertEquals("JOB_CLOSED", event.getEventType());
        assertEquals(1L, event.getJobId());
        assertNotNull(event.getTimestamp());
        assertEquals("CLOSED", event.getDetails().get("status"));
        assertEquals("S2-F4", event.getDetails().get("source"));
    }
}
