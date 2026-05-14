package com.trsh.kpi.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    // NATIONAL (created by Admin, visible everywhere)
    // PROVINCIAL (created by Liaison, visible in their region)
    @Column(nullable = false)
    private String scope;

    // For PROVINCIAL projects — which region owns it
    // For NATIONAL projects — "ALL"
    @Column(nullable = false)
    private String region;

    // Who created this project
    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate targetEndDate;

    // ACTIVE, COMPLETED, ARCHIVED
    @Column(nullable = false)
    private String status;

    // Auto-calculated from milestones
    // 0 to 100
    private int progressPercent;

    // Badge awarded when project hits 100%
    // e.g. "Youth Leadership Summit 2026"
    private String badgeName;

    // Whether badge has been awarded to assigned cadets
    private boolean badgeAwarded;

    // Milestones — owned by this project
    @OneToMany(mappedBy = "project",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<Milestone> milestones = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────

    public ProjectEntity() {}

    public ProjectEntity(String name, String description,
                         String scope, String region,
                         String createdBy, LocalDate startDate,
                         LocalDate targetEndDate, String badgeName) {
        this.name          = name;
        this.description   = description == null ? "" : description;
        this.scope         = scope;
        this.region        = region;
        this.createdBy     = createdBy;
        this.startDate     = startDate;
        this.targetEndDate = targetEndDate;
        this.status        = "ACTIVE";
        this.progressPercent = 0;
        this.badgeName     = badgeName == null ? name : badgeName;
        this.badgeAwarded  = false;
    }

    // ── Progress calculation ───────────────────────────────────
    // Called every time a milestone is marked complete.
    // Recalculates progress based on completed milestones.

    public void recalculateProgress() {
        if (milestones == null || milestones.isEmpty()) {
            this.progressPercent = 0;
            return;
        }
        long completed = milestones.stream()
            .filter(Milestone::isCompleted)
            .count();
        this.progressPercent =
            (int) Math.round((double) completed
                / milestones.size() * 100);

        // Auto-complete when all milestones done
        if (this.progressPercent == 100) {
            this.status = "COMPLETED";
        }
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetEndDate() { return targetEndDate; }
    public void setTargetEndDate(LocalDate targetEndDate) {
        this.targetEndDate = targetEndDate;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getBadgeName() { return badgeName; }
    public void setBadgeName(String badgeName) {
        this.badgeName = badgeName;
    }

    public boolean isBadgeAwarded() { return badgeAwarded; }
    public void setBadgeAwarded(boolean badgeAwarded) {
        this.badgeAwarded = badgeAwarded;
    }

    public List<Milestone> getMilestones() { return milestones; }
    public void setMilestones(List<Milestone> milestones) {
        this.milestones = milestones;
    }

    // ── Helpers ───────────────────────────────────────────────

    public boolean isNational() {
        return "NATIONAL".equals(this.scope);
    }

    public boolean isProvincial() {
        return "REGIONAL".equals(this.scope);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }
}