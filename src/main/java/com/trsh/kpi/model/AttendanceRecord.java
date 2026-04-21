package com.trsh.kpi.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The meeting this record belongs to
    @Column(nullable = false)
    private Long meetingId;

    // The cadet this record belongs to
    @Column(nullable = false)
    private Long cadetId;

    @Column(nullable = false)
    private String cadetName;

    @Column(nullable = false)
    private String cadetCode;

    @Column(nullable = false)
    private String region;

    // PRESENT, ABSENT, LATE
    @Column(nullable = false)
    private String status;

    // Required if status is ABSENT — cannot be left blank
    private String absenceReason;

    @Column(nullable = false)
    private LocalDate meetingDate;

    // Who recorded this entry (PM name)
    @Column(nullable = false)
    private String recordedBy;

    // ── Constructors ──────────────────────────────────────────

    public AttendanceRecord() {}

    public AttendanceRecord(Long meetingId, Long cadetId,
                            String cadetName, String cadetCode,
                            String region, String status,
                            String absenceReason, LocalDate meetingDate,
                            String recordedBy) {
        this.meetingId = meetingId;
        this.cadetId = cadetId;
        this.cadetName = cadetName;
        this.cadetCode = cadetCode;
        this.region = region;
        this.status = status;
        this.absenceReason = absenceReason == null ? "" : absenceReason;
        this.meetingDate = meetingDate;
        this.recordedBy = recordedBy;
    }

    // ── Validation helper ─────────────────────────────────────
    // Ensures absent cadets always have a reason recorded.
    // This is enforced in MeetingService before saving.

    public boolean isValid() {
        if ("ABSENT".equals(this.status)) {
            return this.absenceReason != null &&
                   !this.absenceReason.isBlank();
        }
        return true;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public Long getCadetId() { return cadetId; }
    public void setCadetId(Long cadetId) { this.cadetId = cadetId; }

    public String getCadetName() { return cadetName; }
    public void setCadetName(String cadetName) { this.cadetName = cadetName; }

    public String getCadetCode() { return cadetCode; }
    public void setCadetCode(String cadetCode) { this.cadetCode = cadetCode; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAbsenceReason() { return absenceReason; }
    public void setAbsenceReason(String absenceReason) {
        this.absenceReason = absenceReason;
    }

    public LocalDate getMeetingDate() { return meetingDate; }
    public void setMeetingDate(LocalDate meetingDate) {
        this.meetingDate = meetingDate;
    }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
}