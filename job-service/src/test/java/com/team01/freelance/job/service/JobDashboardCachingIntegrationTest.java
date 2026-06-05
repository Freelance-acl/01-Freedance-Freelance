package com.team01.freelance.job.service;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.job.client.ContractLookupClient;
import com.team01.freelance.job.dto.JobDashboardDTO;
import com.team01.freelance.job.repository.JobAttachmentRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class JobDashboardCachingIntegrationTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private EventSubject jobEventSubject;

    @Test
    void getJobDashboardCachesResultsAndLogsEveryCall() {
        when(jobRepository.findJobDashboard()).thenReturn(List.<Object[]>of(new Object[]{
                7L, "Analytics Platform", 12L, 950.0, 2L, 4.7
        }));

        List<JobDashboardDTO> first = jobService.getJobDashboard();
        List<JobDashboardDTO> second = jobService.getJobDashboard();

        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertEquals(7L, first.get(0).getJobId());
        assertEquals("Analytics Platform", first.get(0).getTitle());
        assertEquals(12L, first.get(0).getTotalProposals());
        assertEquals(950.0, first.get(0).getAverageBidAmount());
        assertEquals(2L, first.get(0).getActiveAttachments());
        assertEquals(4.7, first.get(0).getRating());

        assertEquals(first.get(0).getJobId(), second.get(0).getJobId());
        assertEquals(first.get(0).getTitle(), second.get(0).getTitle());
        assertEquals(first.get(0).getTotalProposals(), second.get(0).getTotalProposals());

        verify(jobRepository, times(1)).findJobDashboard();
        verify(jobEventSubject, times(2)).notifyObservers(eq("DASHBOARD_VIEWED"), any());
    }

    @TestConfiguration
    static class TestMocks {

        @Bean
        @Primary
        JobRepository jobRepositoryMock() {
            return Mockito.mock(JobRepository.class);
        }

        @Bean
        @Primary
        UserRepository userRepositoryMock() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        @Primary
        ContractLookupClient contractLookupClientMock() {
            return Mockito.mock(ContractLookupClient.class);
        }

        @Bean
        @Primary
        JobAttachmentRepository jobAttachmentRepositoryMock() {
            return Mockito.mock(JobAttachmentRepository.class);
        }

        @Bean
        @Primary
        EventSubject jobEventSubjectMock() {
            return Mockito.mock(EventSubject.class);
        }
    }
}
