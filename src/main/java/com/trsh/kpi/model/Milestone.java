package com.trsh.kpi.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "milestones")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The project this milestone belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    // Order in the project e.g. 1, 2, 3, 4
    @Column(nullable = false)
    private int milestoneOrder;

    // Whether this milestone has been completed
    private boolean completed;

    // When it was completed
    private LocalDate completedDate;

    // Who marked it complete
    private String completedBy;

    // Target date for this milestone
    private LocalDate targetDate;

    // ── Constructors ──────────────────────────────────────────

    public Milestone() {}

    public Milestone(ProjectEntity project, String title,
                     String description, int milestoneOrder,
                     LocalDate targetDate) {
        this.project        = project;
        this.title          = title;
        this.description    = description == null ? "" : description;
        this.milestoneOrder = milestoneOrder;
        this.targetDate     = targetDate;
        this.completed      = false;
    }

    // ── Complete a milestone ──────────────────────────────────
    // Called when a Liaison or Admin marks this milestone done.
    // Automatically triggers project progress recalculation.

    public void markComplete(String completedBy) {
        this.completed     = true;
        this.completedDate = LocalDate.now();
        this.completedBy   = completedBy;

        // Recalculate the parent project progress
        if (this.project != null) {
            this.project.recalculateProgress();
        }
    }

    // Undo a milestone completion if marked by mistake
    public void markIncomplete() {
        this.completed     = false;
        this.completedDate = null;
        this.completedBy   = null;

        if (this.project != null) {
            this.project.recalculateProgress();
        }
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public ProjectEntity getProject() { return project; }
    public void setProject(ProjectEntity project) {
        this.project = project;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public int getMilestoneOrder() { return milestoneOrder; }
    public void setMilestoneOrder(int milestoneOrder) {
        this.milestoneOrder = milestoneOrder;
    }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }

    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) {
        this.completedBy = completedBy;
    }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    // ── Helper ────────────────────────────────────────────────

    // Returns true if this milestone is overdue
    public boolean isOverdue() {
        if (completed || targetDate == null) return false;
        return LocalDate.now().isAfter(targetDate);
    }
}