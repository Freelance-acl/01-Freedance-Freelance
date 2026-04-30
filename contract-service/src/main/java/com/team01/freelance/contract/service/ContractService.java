package com.team01.freelance.contract.service;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    public Contract createContract(Contract contract) {
        validateContractReferences(contract.getFreelancerId(), contract.getJobId(), contract.getClientId(), contract.getProposalId());
        return contractRepository.save(contract);
    }

    /**
     * Updates an existing contract with non-null fields from the provided contract details object.
     * Note: Cross-service foreign keys (jobId, freelancerId, clientId, proposalId) are updated directly.
     *
     * @param id The ID of the contract to update
     * @param contractDetails The object containing updated fields
     * @return The updated contract
     * @throws RuntimeException if the contract is not found
     */
    public Contract updateContract(Long id, Contract contractDetails) {

        return contractRepository.findById(id).map(existingContract -> {
                if (contractDetails.getJobId() != null) existingContract.setJobId(contractDetails.getJobId());
                if (contractDetails.getFreelancerId() != null) existingContract.setFreelancerId(contractDetails.getFreelancerId());
                if (contractDetails.getClientId() != null) existingContract.setClientId(contractDetails.getClientId());
                if (contractDetails.getProposalId() != null) existingContract.setProposalId(contractDetails.getProposalId());
                if (contractDetails.getAgreedAmount() != null) existingContract.setAgreedAmount(contractDetails.getAgreedAmount());
                if (contractDetails.getStatus() != null) existingContract.setStatus(contractDetails.getStatus());
                if (contractDetails.getStartDate() != null) existingContract.setStartDate(contractDetails.getStartDate());
                if (contractDetails.getEndDate() != null) existingContract.setEndDate(contractDetails.getEndDate());
                if (contractDetails.getMetadata() != null) existingContract.setMetadata(contractDetails.getMetadata());
            validateContractReferences(
                    existingContract.getFreelancerId(),
                    existingContract.getJobId(),
                    existingContract.getClientId(),
                    existingContract.getProposalId()
            );
            return contractRepository.save(existingContract);
        }).orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
    }

    private void validateContractReferences(Long freelancerId, Long jobId, Long clientId, Long proposalId) {
        validateReferenceExists("users", freelancerId, "Freelancer");
        validateReferenceExists("jobs", jobId, "Job");
        validateReferenceExists("users", clientId, "Client");
        validateReferenceExists("proposals", proposalId, "Proposal");
    }

    private void validateReferenceExists(String tableName, Long id, String label) {
        if (id == null) {
            throw new IllegalArgumentException(label + " id is required");
        }

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM " + tableName + " WHERE id = ?)",
                Boolean.class,
                id
        );

        if (!Boolean.TRUE.equals(exists)) {
            throw new IllegalArgumentException(label + " not found with id: " + id);
        }
    }

    public boolean deleteContractById(Long id) {
        if (!contractRepository.existsById(id)) {
            return false;
        }
        contractRepository.deleteById(id);
        return true;
    }

    public void deleteAllContracts() {
        contractRepository.deleteAll();
    }
}
