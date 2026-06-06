package com.team01.freelance.job.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for interacting with the Proposal Service.
 * Used to reject proposals when a job is closed.
 */
@Component
public class ProposalLookupClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String proposalServiceBaseUrl;

    public ProposalLookupClient(@Value("${proposal.service.base-url}") String proposalServiceBaseUrl) {
        this.proposalServiceBaseUrl = proposalServiceBaseUrl;
    }

    /**
     * Rejects all SUBMITTED proposals for a given job.
     *
     * @param jobId the job ID
     */
    public void rejectProposalsByJobId(Long jobId) {
        try {
            restTemplate.postForObject(
                    proposalServiceBaseUrl + "/api/proposals/reject-by-job/{jobId}",
                    null,
                    Void.class,
                    jobId
            );
        } catch (Exception e) {
            // Log but don't fail if proposal service is unavailable
            // The job closure should still succeed
            System.err.println("Warning: Failed to reject proposals for job " + jobId + ": " + e.getMessage());
        }
    }
}
