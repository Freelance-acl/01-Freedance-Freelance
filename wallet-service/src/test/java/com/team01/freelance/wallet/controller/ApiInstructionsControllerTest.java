package com.team01.freelance.wallet.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiInstructionsControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ApiInstructionsController()).build();
    }

    @Test
    void getInstructions_returnsOkWithEndpoints() throws Exception {
        mockMvc.perform(get("/api/instructions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("API quick-start instructions"))
                .andExpect(jsonPath("$.baseHost").value("http://localhost"))
                .andExpect(jsonPath("$.endpoints.user.url").value("http://localhost:8081/api/users"))
                .andExpect(jsonPath("$.endpoints.payout.url").value("http://localhost:8085/api/payouts"))
                .andExpect(jsonPath("$.notes").isArray());
    }
}
