package com.team01.freelance.proposal.support;

import com.team01.freelance.proposal.dto.FeignContractDTO;
import com.team01.freelance.proposal.dto.FeignJobDTO;
import com.team01.freelance.proposal.dto.FeignUserDTO;

public final class FeignTestFixtures {

    private FeignTestFixtures() {
    }

    public static FeignUserDTO activeFreelancer(Long id) {
        FeignUserDTO user = new FeignUserDTO();
        user.setId(id);
        user.setName("Freelancer " + id);
        user.setRole("FREELANCER");
        user.setStatus("ACTIVE");
        return user;
    }

    public static FeignUserDTO activeClient(Long id) {
        FeignUserDTO user = new FeignUserDTO();
        user.setId(id);
        user.setName("Client " + id);
        user.setRole("CLIENT");
        user.setStatus("ACTIVE");
        return user;
    }

    public static FeignJobDTO openJob(Long id, Long clientId) {
        FeignJobDTO job = new FeignJobDTO();
        job.setId(id);
        job.setClientId(clientId);
        job.setTitle("Test job " + id);
        job.setCategory("WEB_DEV");
        job.setStatus("IN_PROGRESS");
        return job;
    }

    public static FeignJobDTO jobWithStatus(Long id, Long clientId, String status) {
        FeignJobDTO job = openJob(id, clientId);
        job.setStatus(status);
        return job;
    }

    public static FeignContractDTO activeContract(Long id, Long proposalId, double agreedAmount) {
        FeignContractDTO contract = new FeignContractDTO();
        contract.setId(id);
        contract.setProposalId(proposalId);
        contract.setJobId(100L);
        contract.setFreelancerId(200L);
        contract.setAgreedAmount(agreedAmount);
        contract.setStatus("ACTIVE");
        return contract;
    }
}
