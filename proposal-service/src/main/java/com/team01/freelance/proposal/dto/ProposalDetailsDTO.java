package com.team01.freelance.proposal.dto;

import com.team01.freelance.proposal.model.MilestoneStatus;
import com.team01.freelance.proposal.model.ProposalStatus;

import java.util.List;
import java.util.Map;

public class ProposalDetailsDTO {
    private Long proposalId;
    private Long jobId;
    private Long freelancerId;
    private ProposalStatus status;
    private Double bidAmount;
    private Map<String, Object> metadata;
    private List<MilestoneDTO> milestones;
    private Integer totalMilestones;
    private Integer completedMilestones;

    public Long getProposalId() {
        return proposalId;
    }

    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<MilestoneDTO> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<MilestoneDTO> milestones) {
        this.milestones = milestones;
    }

    public Integer getTotalMilestones() {
        return totalMilestones;
    }

    public void setTotalMilestones(Integer totalMilestones) {
        this.totalMilestones = totalMilestones;
    }

    public Integer getCompletedMilestones() {
        return completedMilestones;
    }

    public void setCompletedMilestones(Integer completedMilestones) {
        this.completedMilestones = completedMilestones;
    }

    public static class MilestoneDTO {
        private Long id;
        private Integer milestoneOrder;
        private String title;
        private String description;
        private Double amount;
        private MilestoneStatus status;
        private Map<String, Object> metadata;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getMilestoneOrder() {
            return milestoneOrder;
        }

        public void setMilestoneOrder(Integer milestoneOrder) {
            this.milestoneOrder = milestoneOrder;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public MilestoneStatus getStatus() {
            return status;
        }

        public void setStatus(MilestoneStatus status) {
            this.status = status;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
