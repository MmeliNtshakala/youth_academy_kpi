package com.trsh.kpi.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "flag_events")
public class FlagEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which cadet this event belongs to
    @Column(nullable = false)
    private Long cadetId;

    @Column(nullable = false)
    private String cadetName;

    // The flag stage this event represents
    // Values: YELLOW, ORANGE, RED, RESTORED, NOTICE_SENT, PLAN_AGREED
    @Column(nullable = false)
    private String eventType;

    // Free-text description of what happened
    @Column(nullable = false)
    private String description;

    // Who triggered this action (PM name or system)
    @Column(nullable = false)
    private String triggeredBy;

    @Column(nullable = false)
    private LocalDate eventDate;

    // Optional notes (e.g. re-engagement plan details)
    private String notes;

    // ── Constructors ──────────────────────────────────────────

    public FlagEvent() {}

    public FlagEvent(Long cadetId, String cadetName, String eventType,
                     String description, String triggeredBy,
                     LocalDate eventDate, String notes) {
        this.cadetId = cadetId;
        this.cadetName = cadetName;
        this.eventType = eventType;
        this.description = description;
        this.triggeredBy = triggeredBy;
        this.eventDate = eventDate;
        this.notes = notes == null ? "" : notes;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public Long getCadetId() { return cadetId; }
    public void setCadetId(Long cadetId) { this.cadetId = cadetId; }

    public String getCadetName() { return cadetName; }
    public void setCadetName(String cadetName) { this.cadetName = cadetName; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}