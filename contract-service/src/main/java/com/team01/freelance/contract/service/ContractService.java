package com.team01.freelance.contract.service;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.repository.ContractRepository;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    public Contract createContract(Contract contract) {
        if (contract.getFreelancerId() == null || contract.getJobId() == null ||
            contract.getClientId() == null || contract.getProposalId() == null) {
            throw new IllegalArgumentException("Freelancer, Job, Client, and Proposal IDs are required to create a Contract");
        }

        userRepository.findById(contract.getFreelancerId())
                .orElseThrow(() -> new EntityNotFoundException("Freelancer not found with id: " + contract.getFreelancerId()));

        jobRepository.findById(contract.getJobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + contract.getJobId()));

        userRepository.findById(contract.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + contract.getClientId()));

        proposalRepository.findById(contract.getProposalId())
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found with id: " + contract.getProposalId()));

        return contractRepository.save(contract);
    }

    /**
     * Updates editable fields on an existing contract.
     * Link fields (jobId, freelancerId, clientId, proposalId) are immutable after creation.
     *
     * @param id The ID of the contract to update
     * @param contractDetails The object containing updated fields
     * @return The updated contract
     * @throws EntityNotFoundException if the contract is not found
     */
    public Contract updateContract(Long id, Contract contractDetails) {
        return contractRepository.findById(id).map(existingContract -> {
                if (contractDetails.getAgreedAmount() != null) existingContract.setAgreedAmount(contractDetails.getAgreedAmount());
                if (contractDetails.getStatus() != null) existingContract.setStatus(contractDetails.getStatus());
                if (contractDetails.getStartDate() != null) existingContract.setStartDate(contractDetails.getStartDate());
                if (contractDetails.getEndDate() != null) existingContract.setEndDate(contractDetails.getEndDate());
                if (contractDetails.getMetadata() != null) existingContract.setMetadata(contractDetails.getMetadata());
            return contractRepository.save(existingContract);
        }).orElseThrow(() -> new EntityNotFoundException("Contract not found with id: " + id));
    }

    public boolean deleteContractById(Long id) {
        try {
            contractRepository.deleteById(id);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void deleteAllContracts() {
        contractRepository.deleteAll();
    }
}
