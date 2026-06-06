package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contract.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertJob;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.insertUser;
import static com.team01.freelance.contract.support.ContractJdbcFixtures.saveContract;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ContractF12IntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void milestoneTimeline_returnsMostRecentFirst_andSupportsTimeFilter() throws Exception {
        insertUser(jdbcTemplate, 961L, "Freelancer");
        insertUser(jdbcTemplate, 962L, "Client");
        insertJob(jdbcTemplate, 861L, 962L, "Timeline Job");
        saveContract(jdbcTemplate, 7601L, 861L, 961L, 962L, 1000.0, ContractStatus.ACTIVE,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(2), null, null);

        mockMvc.perform(post("/api/contracts/{id}/milestones/track", 7601L)
                        .contentType("application/json")
                        .content("""
                                {"milestoneOrder":1,"status":"PENDING","recordedBy":"Youssef","notes":"phase started"}
                                """))
                .andExpect(status().isCreated());

        Thread.sleep(10);

        mockMvc.perform(post("/api/contracts/{id}/milestones/track", 7601L)
                        .contentType("application/json")
                        .content("""
                                {"milestoneOrder":1,"status":"IN_PROGRESS","recordedBy":"Youssef","notes":"in progress"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/contracts/{id}/milestones/timeline", 7601L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        String start = LocalDateTime.now().minusMinutes(5).toString();
        String end = LocalDateTime.now().plusMinutes(5).toString();

        mockMvc.perform(get("/api/contracts/{id}/milestones/timeline", 7601L)
                        .param("startTime", start)
                        .param("endTime", end))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
