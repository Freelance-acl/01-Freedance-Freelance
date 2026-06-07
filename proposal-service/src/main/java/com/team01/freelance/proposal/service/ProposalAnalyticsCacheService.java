package com.team01.freelance.proposal.service;

import com.team01.freelance.proposal.dto.ProposalAnalyticsDashboardDTO;
import com.team01.freelance.proposal.repository.ProposalAnalyticsProjection;
import com.team01.freelance.proposal.repository.ProposalRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProposalAnalyticsCacheService {

    private final ProposalRepository proposalRepository;

    public ProposalAnalyticsCacheService(ProposalRepository proposalRepository) {
        this.proposalRepository = proposalRepository;
    }

    @Cacheable(value = "S3-F10", key = "#startDate.toString() + ':' + #endDate.toString()")
    public ProposalAnalyticsDashboardDTO getAnalyticsDashboard(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        ProposalAnalyticsProjection base = proposalRepository.calculateAnalyticsBySubmittedAtRange(start, endExclusive);
        Double avgDays = proposalRepository.getAverageEstimatedDays(start, endExclusive);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : proposalRepository.getProposalCountByStatus(start, endExclusive)) {
            byStatus.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        return ProposalAnalyticsDashboardDTO.builder()
                .totalProposals(base.getTotalProposals().longValue())
                .acceptanceRate(base.getAcceptanceRate().doubleValue())
                .averageBidAmount(base.getAverageBid().doubleValue())
                .averageEstimatedDays(avgDays != null ? avgDays : 0.0)
                .proposalsByStatus(byStatus)
                .build();
    }
}
