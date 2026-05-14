package com.trsh.kpi.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cadet_profiles")
public class CadetProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One profile entry per meeting — full history kept
    @Column(nullable = false)
    private Long cadetId;

    @Column(nullable = false)
    private String cadetName;

    @Column(nullable = false)
    private String cadetCode;

    @Column(nullable = false)
    private String region;

    // ── Wellbeing ─────────────────────────────────────────────
    // Traffic light: GREEN, AMBER, RED
    @Column(nullable = false)
    private String wellbeing;

    // ── Profile fields ────────────────────────────────────────
    // Goals the cadet is working toward in the programme
    @Column(length = 1000)
    private String goals;

    // Current challenges the cadet is facing
    @Column(length = 1000)
    private String challenges;

    // Confidential Liaison notes — not visible to other Liaisons
    @Column(length = 2000)
    private String personalNotes;

    // Pressing information that needs attention
    @Column(length = 1000)
    private String pressingInfo;

    // Date of next scheduled meeting
    private LocalDate nextMeetingDate;

    // Date this profile entry was recorded
    @Column(nullable = false)
    private LocalDate recordedDate;

    // Who recorded this entry
    @Column(nullable = false)
    private String recordedBy;

    // ── Constructors ──────────────────────────────────────────

    public CadetProfile() {}

    public CadetProfile(Long cadetId, String cadetName,
                        String cadetCode, String region,
                        String wellbeing, String goals,
                        String challenges, String personalNotes,
                        String pressingInfo,
                        LocalDate nextMeetingDate,
                        String recordedBy) {
        this.cadetId       = cadetId;
        this.cadetName     = cadetName;
        this.cadetCode     = cadetCode;
        this.region        = region;
        this.wellbeing     = wellbeing;
        this.goals         = goals == null ? "" : goals;
        this.challenges    = challenges == null ? "" : challenges;
        this.personalNotes = personalNotes == null ? "" : personalNotes;
        this.pressingInfo  = pressingInfo == null ? "" : pressingInfo;
        this.nextMeetingDate = nextMeetingDate;
        this.recordedDate  = LocalDate.now();
        this.recordedBy    = recordedBy;
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

    public String getWellbeing() { return wellbeing; }
    public void setWellbeing(String wellbeing) {
        this.wellbeing = wellbeing;
    }

    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }

    public String getChallenges() { return challenges; }
    public void setChallenges(String challenges) {
        this.challenges = challenges;
    }

    public String getPersonalNotes() { return personalNotes; }
    public void setPersonalNotes(String personalNotes) {
        this.personalNotes = personalNotes;
    }

    public String getPressingInfo() { return pressingInfo; }
    public void setPressingInfo(String pressingInfo) {
        this.pressingInfo = pressingInfo;
    }

    public LocalDate getNextMeetingDate() { return nextMeetingDate; }
    public void setNextMeetingDate(LocalDate nextMeetingDate) {
        this.nextMeetingDate = nextMeetingDate;
    }

    public LocalDate getRecordedDate() { return recordedDate; }
    public void setRecordedDate(LocalDate recordedDate) {
        this.recordedDate = recordedDate;
    }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    // ── Helper ────────────────────────────────────────────────

    // Returns true if this profile has pressing info that
    // needs attention — used to highlight on dashboard
    public boolean hasPressingInfo() {
        return this.pressingInfo != null &&
               !this.pressingInfo.isBlank();
    }
}