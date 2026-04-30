package com.team01.freelance.proposal.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Entity
@Table(name = "proposal_milestones")
public class ProposalMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "milestone_order", nullable = false)
    private Integer milestoneOrder;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MilestoneStatus status;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = MilestoneStatus.PENDING;
        }
    }

    // Getters and Setters
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

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }
}

