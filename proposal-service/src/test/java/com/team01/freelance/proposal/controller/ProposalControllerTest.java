package com.team01.freelance.proposal.controller;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.service.ProposalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProposalControllerTest {

    private MockMvc mockMvc;
    private ProposalService proposalService;

    @BeforeEach
    void setUp() {
        ProposalController controller = new ProposalController();
        proposalService = mock(ProposalService.class);
        ReflectionTestUtils.setField(controller, "proposalService", proposalService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllReturnsOk() throws Exception {
        when(proposalService.getAllProposals()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/proposals"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        Proposal proposal = new Proposal();
        when(proposalService.getProposalById(1L)).thenReturn(Optional.of(proposal));

        mockMvc.perform(get("/api/proposals/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsOk() throws Exception {
        Proposal proposal = new Proposal();
        when(proposalService.createProposal(any(Proposal.class))).thenReturn(proposal);

        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsOk() throws Exception {
        Proposal proposal = new Proposal();
        when(proposalService.updateProposal(eq(1L), any(Proposal.class))).thenReturn(proposal);

        mockMvc.perform(put("/api/proposals/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(proposalService.deleteProposalById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/proposals/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(proposalService).deleteAllProposals();

        mockMvc.perform(delete("/api/proposals/all"))
                .andExpect(status().isNoContent());
    }
}
