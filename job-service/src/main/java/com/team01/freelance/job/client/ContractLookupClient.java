package com.team01.freelance.job.client;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class ContractLookupClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String contractServiceBaseUrl;

    public ContractLookupClient(@Value("${contract.service.base-url}") String contractServiceBaseUrl) {
        this.contractServiceBaseUrl = contractServiceBaseUrl;
    }

    public ContractSummary getContractById(Long contractId) {
        try {
            ContractSummary contract = restTemplate.getForObject(
                    contractServiceBaseUrl + "/api/contracts/{id}",
                    ContractSummary.class,
                    contractId
            );

            if (contract == null) {
                throw new IllegalStateException("Contract service returned an empty response for id: " + contractId);
            }

            return contract;
        } catch (HttpClientErrorException.NotFound e) {
            throw new EntityNotFoundException("Contract not found with id: " + contractId);
        }
    }

    /**
     * Checks if there is an ACTIVE contract for a given job.
     *
     * @param jobId the job ID
     * @return true if an ACTIVE contract exists, false otherwise
     */
    public boolean hasActiveContract(Long jobId) {
        try {
            Boolean hasActive = restTemplate.getForObject(
                    contractServiceBaseUrl + "/api/contracts/jobs/{jobId}/has-active",
                    Boolean.class,
                    jobId
            );
            return hasActive != null && hasActive;
        } catch (Exception e) {
            // If contract service is unavailable or returns error, assume no active contract
            return false;
        }
    }
}
