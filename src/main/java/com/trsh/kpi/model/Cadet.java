package com.trsh.kpi.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cadets")
public class Cadet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cadetCode;       // e.g. CDT-0001

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String project;         // Leadership, Community, Research, Operations

    @Column(nullable = false)
    private String flagStatus;      // NONE, YELLOW, ORANGE, RED

    @Column(nullable = false)
    private int attendancePercent;  // Attendance in percentile

    @Column(nullable = false)
    private String projectManager;

    private LocalDate lastContactDate;

    private int daysSinceFlag;

    private int orangeFlagCount;    // tracks how many Orange flags in 12 months

    private boolean underCorrection; // checks: true if RED flag correction is active

    private String reengagementPlan; // notes from mentor meeting

    // ── Constructors ──────────────────────────────────────────

    public Cadet() {}

    public Cadet(String cadetCode, String fullName, String project,
                 String flagStatus, int attendancePercent,
                 String projectManager, LocalDate lastContactDate) {
        this.cadetCode = cadetCode;
        this.fullName = fullName;
        this.project = project;
        this.flagStatus = flagStatus;
        this.attendancePercent = attendancePercent;
        this.projectManager = projectManager;
        this.lastContactDate = lastContactDate;
        this.daysSinceFlag = 0;
        this.orangeFlagCount = 0;
        this.underCorrection = false;
        this.reengagementPlan = "";
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public String getCadetCode() { return cadetCode; }
    public void setCadetCode(String cadetCode) { this.cadetCode = cadetCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    public String getFlagStatus() { return flagStatus; }
    public void setFlagStatus(String flagStatus) { this.flagStatus = flagStatus; }

    public int getAttendancePercent() { return attendancePercent; }
    public void setAttendancePercent(int attendancePercent) { this.attendancePercent = attendancePercent; }

    public String getProjectManager() { return projectManager; }
    public void setProjectManager(String projectManager) { this.projectManager = projectManager; }

    public LocalDate getLastContactDate() { return lastContactDate; }
    public void setLastContactDate(LocalDate lastContactDate) { this.lastContactDate = lastContactDate; }

    public int getDaysSinceFlag() { return daysSinceFlag; }
    public void setDaysSinceFlag(int daysSinceFlag) { this.daysSinceFlag = daysSinceFlag; }

    public int getOrangeFlagCount() { return orangeFlagCount; }
    public void setOrangeFlagCount(int orangeFlagCount) { this.orangeFlagCount = orangeFlagCount; }

    public boolean isUnderCorrection() { return underCorrection; }
    public void setUnderCorrection(boolean underCorrection) { this.underCorrection = underCorrection; }

    public String getReengagementPlan() { return reengagementPlan; }
    public void setReengagementPlan(String reengagementPlan) { this.reengagementPlan = reengagementPlan; }
}