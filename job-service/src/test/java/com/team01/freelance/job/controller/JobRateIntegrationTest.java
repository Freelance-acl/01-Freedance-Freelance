package com.team01.freelance.job.controller;

import com.team01.freelance.job.client.ContractLookupClient;
import com.team01.freelance.job.client.ContractSummary;
import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.job.support.AbstractIntegrationTest;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F9] Integration tests for {@code POST /api/jobs/{id}/rate}.
 */
@Transactional
@Import(JobRateIntegrationTest.ContractLookupTestConfig.class)
class JobRateIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ContractLookupClient contractLookupClient;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private Job job;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        User client = saveUser("Client", UserRole.CLIENT);
        job = saveJob(client.getId());
    }

    @Test
    void rateJob_updatesRunningAverageFromCompletedContract() throws Exception {
        ContractSummary contract = new ContractSummary();
        contract.setJobId(job.getId());
        contract.setStatus("COMPLETED");
        when(contractLookupClient.getContractById(eq(99L))).thenReturn(contract);

        mockMvc.perform(post("/api/jobs/{id}/rate", job.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":99,\"rating\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.totalRatings").value(1));
    }

    @TestConfiguration
    static class ContractLookupTestConfig {
        @Bean
        @Primary
        ContractLookupClient contractLookupClient() {
            return mock(ContractLookupClient.class);
        }
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("rate-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+2700" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Job saveJob(Long clientId) {
        Job job = new Job();
        job.setClientId(clientId);
        job.setTitle("Rate job");
        job.setDescription("Rate job test");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(500.0);
        job.setRating(0.0);
        job.setTotalRatings(0);
        return jobRepository.save(job);
    }
}
