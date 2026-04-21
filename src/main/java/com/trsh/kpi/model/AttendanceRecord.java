package com.trsh.kpi.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "Sunday Session" or "Special Event"
    @Column(nullable = false)
    private String meetingType;

    // Title e.g. "Sunday Session — 20 Apr 2026"
    @Column(nullable = false)
    private String title;

    // Which region this meeting is for, or "ALL" for all regions
    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private LocalDate meetingDate;

    // Who created this meeting
    @Column(nullable = false)
    private String createdBy;

    // Whether register has been completed
    private boolean registerComplete;

    private String notes;



    public Meeting() {}

    public Meeting(String meetingType, String title, String region,
                   LocalDate meetingDate, String createdBy, String notes) {
        this.meetingType = meetingType;
        this.title = title;
        this.region = region;
        this.meetingDate = meetingDate;
        this.createdBy = createdBy;
        this.registerComplete = false;
        this.notes = notes == null ? "" : notes;
    }



    public Long getId() { return id; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public LocalDate getMeetingDate() { return meetingDate; }
    public void setMeetingDate(LocalDate meetingDate) { this.meetingDate = meetingDate; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public boolean isRegisterComplete() { return registerComplete; }
    public void setRegisterComplete(boolean registerComplete) {
        this.registerComplete = registerComplete;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}