package com.team01.freelance.job.controller;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentType;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F7] Integration tests for {@code GET /api/jobs/attachments/expired}.
 */
@Transactional
class JobExpiredAttachmentsIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        User client = saveUser("Client", UserRole.CLIENT);
        saveJobWithExpiredAttachment(client.getId(), "Expired brief job");
    }

    @Test
    void expiredAttachments_returnsJobsWithExpiredAttachmentAlerts() throws Exception {
        mockMvc.perform(get("/api/jobs/attachments/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].jobTitle").value("Expired brief job"))
                .andExpect(jsonPath("$[0].expiredCount").value(1))
                .andExpect(jsonPath("$[0].expiredAttachments.length()").value(1));
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("expired-att-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+2500" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void saveJobWithExpiredAttachment(Long clientId, String title) {
        Job job = new Job();
        job.setClientId(clientId);
        job.setTitle(title);
        job.setDescription("Expired attachment test");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(500.0);
        job.setJobAttachments(new ArrayList<>());

        JobAttachment attachment = new JobAttachment();
        attachment.setType(JobAttachmentType.BRIEF);
        attachment.setFileUrl("https://example.com/brief.pdf");
        attachment.setExpiryDate(LocalDate.now().minusDays(2));
        attachment.setVerified(false);
        attachment.setJob(job);
        job.getJobAttachments().add(attachment);

        jobRepository.save(job);
    }
}
