package com.team01.freelance.proposal.controller;

import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.service.ProposalMilestoneService;
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

class ProposalMilestoneControllerTest {

    private MockMvc mockMvc;
    private ProposalMilestoneService proposalMilestoneService;

    @BeforeEach
    void setUp() {
        ProposalMilestoneController controller = new ProposalMilestoneController();
        proposalMilestoneService = mock(ProposalMilestoneService.class);
        ReflectionTestUtils.setField(controller, "proposalMilestoneService", proposalMilestoneService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllReturnsOk() throws Exception {
        when(proposalMilestoneService.getAllProposalMilestones()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/proposal-milestones"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturnsOk() throws Exception {
        ProposalMilestone proposalMilestone = new ProposalMilestone();
        when(proposalMilestoneService.getProposalMilestoneById(1L)).thenReturn(Optional.of(proposalMilestone));

        mockMvc.perform(get("/api/proposal-milestones/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsOk() throws Exception {
        ProposalMilestone proposalMilestone = new ProposalMilestone();
        when(proposalMilestoneService.createProposalMilestone(any(ProposalMilestone.class))).thenReturn(proposalMilestone);

        mockMvc.perform(post("/api/proposal-milestones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsOk() throws Exception {
        ProposalMilestone proposalMilestone = new ProposalMilestone();
        when(proposalMilestoneService.updateProposalMilestone(eq(1L), any(ProposalMilestone.class))).thenReturn(proposalMilestone);

        mockMvc.perform(put("/api/proposal-milestones/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteByIdReturnsNoContent() throws Exception {
        when(proposalMilestoneService.deleteProposalMilestoneById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/proposal-milestones/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAllReturnsNoContent() throws Exception {
        doNothing().when(proposalMilestoneService).deleteAllProposalMilestones();

        mockMvc.perform(delete("/api/proposal-milestones/all"))
                .andExpect(status().isNoContent());
    }
}
