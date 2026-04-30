package com.team01.freelance.contract.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController()).build();
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/contracts/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
    @Test
    void indexEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/contracts"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome to Freelance Contract Service"));
    }
}
