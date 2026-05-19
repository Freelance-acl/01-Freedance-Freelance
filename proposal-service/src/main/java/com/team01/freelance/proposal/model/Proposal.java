package com.team01.freelance.proposal.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "proposals")
public class Proposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    @JsonAlias({"jobId", "job_id"})
    private Long jobId;

    @Column(name = "freelancer_id", nullable = false)
    @JsonAlias({"freelancerId", "freelancer_id"})
    private Long freelancerId;

    @Column(name = "cover_letter", nullable = false)
    @JsonAlias({"coverLetter", "cover_letter"})
    private String coverLetter;

    @Column(name = "bid_amount", nullable = false)
    @JsonAlias({"bidAmount", "bid_amount"})
    private Double bidAmount;

    @Column(name = "estimated_days", nullable = false)
    @JsonAlias({"estimatedDays", "estimated_days"})
    private Integer estimatedDays;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProposalStatus status;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    @Column(name = "submitted_at", nullable = false)
    @JsonAlias({"submittedAt", "submitted_at"})
    private LocalDateTime submittedAt;

    @Column(name = "accepted_at")
    @JsonAlias({"acceptedAt", "accepted_at"})
    private LocalDateTime acceptedAt;

    @JsonIgnore
    @OrderBy("milestoneOrder ASC")
    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalMilestone> proposalMilestones;

    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = ProposalStatus.SUBMITTED;
        }
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        if (proposalMilestones == null) {
            proposalMilestones = new ArrayList<>();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public Integer getEstimatedDays() {
        return estimatedDays;
    }

    public void setEstimatedDays(Integer estimatedDays) {
        this.estimatedDays = estimatedDays;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    @JsonProperty("proposalMilestones")
    public List<ProposalMilestone> getProposalMilestones() {
        if (proposalMilestones == null || !Hibernate.isInitialized(proposalMilestones)) {
            return List.of();
        }
        return proposalMilestones.stream()
                .sorted(Comparator.comparing(ProposalMilestone::getMilestoneOrder))
                .toList();
    }

    /** Returns the live milestone collection for persistence updates. */
    public List<ProposalMilestone> getProposalMilestonesForUpdate() {
        ensureMutableMilestones();
        return proposalMilestones;
    }

    public void setProposalMilestones(List<ProposalMilestone> proposalMilestones) {
        this.proposalMilestones = proposalMilestones;
        if (this.proposalMilestones != null) {
            this.proposalMilestones.forEach(milestone -> milestone.setProposal(this));
        }
    }

    public void addProposalMilestone(ProposalMilestone proposalMilestone) {
        ensureMutableMilestones();
        proposalMilestone.setProposal(this);
        proposalMilestones.add(proposalMilestone);
    }

    private void ensureMutableMilestones() {
        if (proposalMilestones == null) {
            proposalMilestones = new ArrayList<>();
        }
    }
}
