package com.team01.freelance.contract.service;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    public Contract createContract(Contract contract) {
        return contractRepository.save(contract);
    }

    public Optional<Contract> updateContract(Long id, Contract contractDetails) {
        return contractRepository.findById(id).map(existingContract -> {
            if (contractDetails.getJobId() != null) {
                existingContract.setJobId(contractDetails.getJobId());
            }
            if (contractDetails.getFreelancerId() != null) {
                existingContract.setFreelancerId(contractDetails.getFreelancerId());
            }
            if (contractDetails.getClientId() != null) {
                existingContract.setClientId(contractDetails.getClientId());
            }
            if (contractDetails.getProposalId() != null) {
                existingContract.setProposalId(contractDetails.getProposalId());
            }
            if (contractDetails.getAgreedAmount() != null) {
                existingContract.setAgreedAmount(contractDetails.getAgreedAmount());
            }
            if (contractDetails.getStatus() != null) {
                existingContract.setStatus(contractDetails.getStatus());
            }
            if (contractDetails.getStartDate() != null) {
                existingContract.setStartDate(contractDetails.getStartDate());
            }
            if (contractDetails.getEndDate() != null) {
                existingContract.setEndDate(contractDetails.getEndDate());
            }
            if (contractDetails.getMetadata() != null) {
                existingContract.setMetadata(contractDetails.getMetadata());
            }
            return contractRepository.save(existingContract);
        });
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
