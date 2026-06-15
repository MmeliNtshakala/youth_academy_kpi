package com.trsh.kpi.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cadet_projects")
public class CadetProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The cadet assigned to this project
    @Column(nullable = false)
    private Long cadetId;

    @Column(nullable = false)
    private String cadetName;

    @Column(nullable = false)
    private String cadetCode;

    @Column(nullable = false)
    private String region;

    // The project this cadet is assigned to
    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String projectName;

    @Column(nullable = false)
    private String projectScope; // NATIONAL or PROVINCIAL

    // Who assigned this cadet to the project
    @Column(nullable = false)
    private String assignedBy;

    @Column(nullable = false)
    private LocalDate assignedDate;

    // ── Badge fields ──────────────────────────────────────────
    // Populated automatically when project hits 100%
    // and badge is awarded

    private boolean badgeEarned;

    // The badge name e.g. "Youth Leadership Summit 2026"
    private String badgeName;

    // When the badge was awarded
    private LocalDate badgeAwardedDate;

    // ── Cadet participation status ────────────────────────────
    // ACTIVE — currently participating
    // COMPLETED — finished the project
    // WITHDRAWN — removed from project
    @Column(nullable = false)
    private String participationStatus;

    // Optional notes about this cadet's role in the project
    @Column(length = 500)
    private String notes;

    // ── Constructors ──────────────────────────────────────────

    public CadetProject() {}

    public CadetProject(Long cadetId, String cadetName,
                        String cadetCode, String region,
                        Long projectId, String projectName,
                        String projectScope, String assignedBy) {
        this.cadetId            = cadetId;
        this.cadetName          = cadetName;
        this.cadetCode          = cadetCode;
        this.region             = region;
        this.projectId          = projectId;
        this.projectName        = projectName;
        this.projectScope       = projectScope;
        this.assignedBy         = assignedBy;
        this.assignedDate       = LocalDate.now();
        this.participationStatus = "ACTIVE";
        this.badgeEarned        = false;
        this.notes              = "";
    }

    // ── Award badge ───────────────────────────────────────────
    // Called automatically by CadetProfileService when
    // a project hits 100% completion.
    // Sets badgeEarned = true and records the badge name
    // and award date on this cadet's project record.

    public void awardBadge(String badgeName) {
        this.badgeEarned      = true;
        this.badgeName        = badgeName;
        this.badgeAwardedDate = LocalDate.now();
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public Long getCadetId() { return cadetId; }
    public void setCadetId(Long cadetId) { this.cadetId = cadetId; }

    public String getCadetName() { return cadetName; }
    public void setCadetName(String cadetName) {
        this.cadetName = cadetName;
    }

    public String getCadetCode() { return cadetCode; }
    public void setCadetCode(String cadetCode) {
        this.cadetCode = cadetCode;
    }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectScope() { return projectScope; }
    public void setProjectScope(String projectScope) {
        this.projectScope = projectScope;
    }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDate getAssignedDate() { return assignedDate; }
    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public boolean isBadgeEarned() { return badgeEarned; }
    public void setBadgeEarned(boolean badgeEarned) {
        this.badgeEarned = badgeEarned;
    }

    public String getBadgeName() { return badgeName; }
    public void setBadgeName(String badgeName) {
        this.badgeName = badgeName;
    }

    public LocalDate getBadgeAwardedDate() { return badgeAwardedDate; }
    public void setBadgeAwardedDate(LocalDate badgeAwardedDate) {
        this.badgeAwardedDate = badgeAwardedDate;
    }

    public String getParticipationStatus() {
        return participationStatus;
    }
    public void setParticipationStatus(String participationStatus) {
        this.participationStatus = participationStatus;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}