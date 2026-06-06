package com.team01.freelance.user.adapter;

import com.team01.freelance.user.dto.UserContractSummaryDTO;
import com.team01.freelance.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class ObjectArrayDtoAdapter {

    /**
     * Adapts the raw native-SQL Object[] row from the S1-F3 contract-summary
     * query into a UserContractSummaryDTO. Column order matches the SELECT in
     * UserRepository.getUserContractSummary:
     *   [0] total_contracts, [1] completed_contracts, [2] terminated_contracts,
     *   [3] total_earnings, [4] average_contract_value
     */
    public UserContractSummaryDTO adapt(Object[] row, User user) {
        if (row == null) {
            return UserContractSummaryDTO.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .totalContracts(0L)
                    .completedContracts(0L)
                    .terminatedContracts(0L)
                    .totalEarnings(0.0)
                    .averageContractValue(0.0)
                    .build();
        }
        return UserContractSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalContracts(row[0] != null ? ((Number) row[0]).longValue() : 0L)
                .completedContracts(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                .terminatedContracts(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                .totalEarnings(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                .averageContractValue(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                .build();
    }
}