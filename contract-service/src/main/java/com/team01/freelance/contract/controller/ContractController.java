package com.team01.freelance.contract.controller;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team01.freelance.contract.dto.StalledContractDTO;
import com.team01.freelance.contract.service.ContractService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @GetMapping
    public ResponseEntity<List<Contract>> getAllContracts() {
        return ResponseEntity.ok(contractService.getAllContracts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable Long id) {
        return contractService.getContractById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<Contract> getActiveContractForUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(contractService.getActiveContractForUser(userId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Contract> createContract(@RequestBody Contract contract) {
        try {
            return ResponseEntity.ok(contractService.createContract(contract));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Updates a contract by ID.
     *
     * @param id the contract ID
     * @param contract the update payload
     * @return 200 with updated contract, 400 for invalid references, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Contract> updateContract(@PathVariable Long id, @RequestBody Contract contract) {
        try {
            return ResponseEntity.ok(contractService.updateContract(id, contract));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            // Maps to the "Contract not found" exception from service
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{contractId}/progress")
    public ResponseEntity<Contract> updateContractProgress(
            @PathVariable Long contractId,
            @RequestBody Map<String, Object> metadataUpdates
    ) {
        try {
            return ResponseEntity.ok(contractService.updateContractProgress(contractId, metadataUpdates));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContractById(@PathVariable Long id) {
        if (contractService.deleteContractById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllContracts() {
        contractService.deleteAllContracts();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Long>> purgeOldContractData(@RequestParam Integer olderThanDays) {
        try {
            long deletedCount = contractService.purgeOldContractData(olderThanDays);
            return ResponseEntity.ok(Map.of("deletedCount", deletedCount));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/freelancer/{freelancerId}/summary")
    public ResponseEntity<FreelancerPerformanceDTO> getFreelancerPerformanceSummary(
            @PathVariable Long freelancerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            return ResponseEntity.ok(contractService.getFreelancerPerformanceSummary(freelancerId, startDate, endDate));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stalled")
    public ResponseEntity<List<StalledContractDTO>> getStalledContracts(
            @RequestParam Double maxProgress,
            @RequestParam Integer stalledDays
    ) {
        try {
            return ResponseEntity.ok(contractService.findStalledContracts(maxProgress, stalledDays));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
