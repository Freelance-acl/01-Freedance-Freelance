package com.team01.freelance.proposal.controller;

import com.team01.freelance.proposal.dto.ProposalDetailsDTO;
import com.team01.freelance.proposal.dto.ProposalAnalyticsDTO;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalMilestone;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.service.ProposalService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    void searchReturnsOk() throws Exception {
        when(proposalService.searchProposals(isNull(), eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31))))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/proposals/search")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    void searchWithStatusDelegatesToService() throws Exception {
        when(proposalService.searchProposals(eq("ACCEPTED"), eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31))))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/proposals/search")
                        .param("status", "ACCEPTED")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    void analyticsReturnsDtoAndDelegatesToService() throws Exception {
        when(proposalService.getProposalAnalytics(eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31))))
                .thenReturn(new ProposalAnalyticsDTO(10, 4, 3, 7100.0, 710.0, 40.0));

        mockMvc.perform(get("/api/proposals/analytics")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProposals").value(10))
                .andExpect(jsonPath("$.acceptedProposals").value(4))
                .andExpect(jsonPath("$.rejectedProposals").value(3))
                .andExpect(jsonPath("$.totalBidValue").value(7100.0))
                .andExpect(jsonPath("$.averageBid").value(710.0))
                .andExpect(jsonPath("$.acceptanceRate").value(40.0));

        verify(proposalService).getProposalAnalytics(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
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
    void getDetailsReturnsOk() throws Exception {
        ProposalDetailsDTO details = new ProposalDetailsDTO();
        details.setProposalId(1L);
        details.setMilestones(List.of());
        details.setTotalMilestones(0);
        details.setCompletedMilestones(0);
        when(proposalService.getProposalDetails(1L)).thenReturn(details);

        mockMvc.perform(get("/api/proposals/{proposalId}/details", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void getDetailsReturnsNotFoundWhenProposalDoesNotExist() throws Exception {
        when(proposalService.getProposalDetails(404L)).thenThrow(new EntityNotFoundException("Proposal not found"));

        mockMvc.perform(get("/api/proposals/{proposalId}/details", 404L))
                .andExpect(status().isNotFound());
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
    void acceptReturnsOk() throws Exception {
        Proposal proposal = new Proposal();
        when(proposalService.acceptProposal(1L)).thenReturn(proposal);

        mockMvc.perform(put("/api/proposals/{id}/accept", 1L))
    void addMilestonesReturnsOk() throws Exception {
        Proposal proposal = new Proposal();
        ProposalMilestone milestone = new ProposalMilestone();
        milestone.setMilestoneOrder(1);
        proposal.setProposalMilestones(new ArrayList<>(List.of(milestone)));
        when(proposalService.addMilestones(eq(1L), any())).thenReturn(proposal);

        mockMvc.perform(post("/api/proposals/{proposalId}/milestones", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"title\":\"Planning\",\"description\":\"Plan\",\"amount\":800.0}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalMilestones[0].milestoneOrder").value(1));
    }

    @Test
    void addMilestonesReturnsNotFoundWhenProposalMissing() throws Exception {
        when(proposalService.addMilestones(eq(404L), any()))
                .thenThrow(new EntityNotFoundException("Proposal not found"));

        mockMvc.perform(post("/api/proposals/{proposalId}/milestones", 404L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"title\":\"Planning\",\"description\":\"Plan\",\"amount\":800.0}]"))
                .andExpect(status().isNotFound());
    }

    @Test
    void withdrawReturnsOk() throws Exception {
        Proposal proposal = new Proposal();
        proposal.setStatus(ProposalStatus.WITHDRAWN);
        when(proposalService.withdrawProposal(1L)).thenReturn(proposal);

        mockMvc.perform(put("/api/proposals/{id}/withdraw", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void acceptReturnsNotFound() throws Exception {
        when(proposalService.acceptProposal(1L))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Proposal not found"));

        mockMvc.perform(put("/api/proposals/{id}/accept", 1L))
    void withdrawReturnsNotFoundWhenProposalMissing() throws Exception {
        when(proposalService.withdrawProposal(404L))
                .thenThrow(new EntityNotFoundException("Proposal not found"));

        mockMvc.perform(put("/api/proposals/{id}/withdraw", 404L))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptReturnsBadRequest() throws Exception {
        when(proposalService.acceptProposal(1L))
                .thenThrow(new IllegalArgumentException("Only SUBMITTED or SHORTLISTED proposals can be accepted"));

        mockMvc.perform(put("/api/proposals/{id}/accept", 1L))
    void addMilestonesReturnsBadRequestForInvalidMilestone() throws Exception {
        when(proposalService.addMilestones(eq(2L), any()))
                .thenThrow(new IllegalArgumentException("Milestone title is required"));

        mockMvc.perform(post("/api/proposals/{proposalId}/milestones", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"description\":\"Plan\",\"amount\":800.0}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdrawReturnsBadRequestWhenStatusCannotBeWithdrawn() throws Exception {
        when(proposalService.withdrawProposal(2L))
                .thenThrow(new IllegalArgumentException("Only SUBMITTED or SHORTLISTED proposals can be withdrawn"));

        mockMvc.perform(put("/api/proposals/{id}/withdraw", 2L))
                .andExpect(status().isBadRequest());
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

    @Test
    void searchByMetadataReturnsOk() throws Exception {
        when(proposalService.searchProposalsByMetadata("approach", "agile")).thenReturn(List.of());

        mockMvc.perform(get("/api/proposals/metadata/search")
                        .param("key", "approach")
                        .param("value", "agile"))
                .andExpect(status().isOk());
    }

    @Test
    void searchByMetadataReturns400WhenInvalid() throws Exception {
        when(proposalService.searchProposalsByMetadata("", "x"))
                .thenThrow(new IllegalArgumentException("key and value are required"));

        mockMvc.perform(get("/api/proposals/metadata/search")
                        .param("key", "")
                        .param("value", "x"))
                .andExpect(status().isBadRequest());
    }
}
