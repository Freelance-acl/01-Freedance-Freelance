package com.team01.freelance.proposal.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
import com.team01.freelance.proposal.support.ProposalTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * [S3-F5] Integration tests for {@code GET /api/proposals/metadata/search}.
 */
@Transactional
class ProposalMetadataSearchIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProposalRepository proposalRepository;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        proposalRepository.deleteAll();
    }

    @Test
    void searchByMetadata_returnsMatchingProposals() throws Exception {
        Proposal agile = saveProposal(101L, Map.of("approach", "agile"));
        saveProposal(102L, Map.of("approach", "waterfall"));
        saveProposal(103L, Map.of("approach", "waterfall"));

        mockMvc.perform(get("/api/proposals/metadata/search")
                        .param("key", "approach")
                        .param("value", "agile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(agile.getId().intValue()))
                .andExpect(jsonPath("$[0].metadata.approach").value("agile"));
    }

    @Test
    void searchByMetadata_blankKey_returns400() throws Exception {
        mockMvc.perform(get("/api/proposals/metadata/search")
                        .param("key", "")
                        .param("value", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchByMetadata_blankValue_returns400() throws Exception {
        mockMvc.perform(get("/api/proposals/metadata/search")
                        .param("key", "approach")
                        .param("value", "   "))
                .andExpect(status().isBadRequest());
    }

    private Proposal saveProposal(Long jobId, Map<String, Object> metadata) {
        Map<String, Object> storedMetadata = new LinkedHashMap<>(metadata);
        Proposal proposal = new Proposal();
        proposal.setJobId(jobId);
        proposal.setFreelancerId(ProposalTestData.FREELANCER_ID);
        proposal.setCoverLetter("Metadata test proposal");
        proposal.setBidAmount(2000.0);
        proposal.setEstimatedDays(14);
        proposal.setStatus(ProposalStatus.SUBMITTED);
        proposal.setMetadata(storedMetadata);
        return proposalRepository.save(proposal);
    }
}
