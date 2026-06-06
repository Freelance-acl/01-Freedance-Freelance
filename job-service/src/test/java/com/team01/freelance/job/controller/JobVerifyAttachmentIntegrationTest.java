package com.team01.freelance.job.controller;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentType;
import com.team01.freelance.job.model.JobCategory;
import com.team01.freelance.job.model.JobStatus;
import com.team01.freelance.job.repository.JobAttachmentRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.job.support.AbstractIntegrationTest;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S2-F8] Integration tests for {@code PUT /api/jobs/{jobId}/attachments/{attachmentId}/verify}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class JobVerifyAttachmentIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobAttachmentRepository jobAttachmentRepository;

    @Autowired
    private UserRepository userRepository;

    private Job job;
    private JobAttachment attachment;
    private User admin;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        User client = saveUser("Client", UserRole.CLIENT);
        admin = saveUser("Admin", UserRole.ADMIN);
        job = saveJobWithValidAttachment(client.getId());
        attachment = jobAttachmentRepository.findAll().stream()
                .filter(a -> job.getId().equals(a.getJob().getId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void verifyAttachment_marksAttachmentVerifiedWithMetadata() throws Exception {
        mockMvc.perform(put("/api/jobs/{jobId}/attachments/{attachmentId}/verify", job.getId(), attachment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedBy\":" + admin.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobAttachments[0].verified").value(true))
                .andExpect(jsonPath("$.jobAttachments[0].metadata.verifiedBy").value(admin.getId().intValue()));
    }

    private User saveUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("verify-" + System.nanoTime() + "@test.dev");
        user.setPassword("secret");
        user.setPhone("+2600" + (System.nanoTime() % 1_000_000_000L));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Job saveJobWithValidAttachment(Long clientId) {
        Job job = new Job();
        job.setClientId(clientId);
        job.setTitle("Verify attachment job");
        job.setDescription("Verify attachment test");
        job.setCategory(JobCategory.WEB_DEV);
        job.setStatus(JobStatus.OPEN);
        job.setBudgetMin(100.0);
        job.setBudgetMax(500.0);
        job.setJobAttachments(new ArrayList<>());

        JobAttachment attachment = new JobAttachment();
        attachment.setType(JobAttachmentType.BRIEF);
        attachment.setFileUrl("https://example.com/brief.pdf");
        attachment.setExpiryDate(LocalDate.now().plusDays(30));
        attachment.setVerified(false);
        attachment.setJob(job);
        job.getJobAttachments().add(attachment);

        return jobRepository.save(job);
    }
}