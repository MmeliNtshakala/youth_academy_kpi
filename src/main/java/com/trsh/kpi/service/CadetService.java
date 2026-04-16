package com.trsh.kpi.service;

import com.trsh.kpi.model.Cadet;
import com.trsh.kpi.model.FlagEvent;
import com.trsh.kpi.reposatory.CadetRepository;
import com.trsh.kpi.reposatory.FlagEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CadetService {

    private final CadetRepository cadetRepo;
    private final FlagEventRepository flagEventRepo;

    public CadetService(CadetRepository cadetRepo,
                        FlagEventRepository flagEventRepo) {
        this.cadetRepo = cadetRepo;
        this.flagEventRepo = flagEventRepo;
    }

    // ── Cadet CRUD ────────────────────────────────────────────

    public List<Cadet> getAllCadets() {
        return cadetRepo.findAll();
    }

    public Optional<Cadet> getCadetById(Long id) {
        return cadetRepo.findById(id);
    }

    public Optional<Cadet> getCadetByCode(String code) {
        return cadetRepo.findByCadetCode(code);
    }

    public List<Cadet> getCadetsByProject(String project) {
        return cadetRepo.findByProject(project);
    }

    public List<Cadet> getCadetsByFlag(String flagStatus) {
        return cadetRepo.findByFlagStatus(flagStatus);
    }

    public List<Cadet> searchCadets(String name) {
        return cadetRepo.findByFullNameContainingIgnoreCase(name);
    }

    public Cadet saveCadet(Cadet cadet) {
        return cadetRepo.save(cadet);
    }

    public void deleteCadet(Long id) {
        cadetRepo.deleteById(id);
    }

    // ── Flag History ──────────────────────────────────────────

    public List<FlagEvent> getFlagHistory(Long cadetId) {
        return flagEventRepo.findByCadetIdOrderByEventDateDesc(cadetId);
    }

    public List<FlagEvent> getRecentActivity() {
        return flagEventRepo.findTop10ByOrderByEventDateDesc();
    }

    // ── FLAG ESCALATION LOGIC ─────────────────────────────────
    // This section enforces the three-stage framework from the
    // TRSH Cadet Participation & Accountability Framework document.

    /**
     * STAGE 1 — Issue a Yellow Flag.
     *
     * Trigger: Cadet missed 2 consecutive meetings OR
     *          no response to communications for 14 days.
     *
     * Action:  Formal written notice issued.
     *          Cadet given 5 days to re-engage.
     *          Flag recorded against cadet profile.
     */
    public Map<String, Object> issueYellowFlag(Long cadetId,
                                                String triggeredBy,
                                                String reason) {
        Map<String, Object> result = new HashMap<>();

        Optional<Cadet> optional = cadetRepo.findById(cadetId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cadet not found.");
            return result;
        }

        Cadet cadet = optional.get();

        // Cannot flag a cadet who is already at Orange or Red
        if (cadet.getFlagStatus().equals("ORANGE") ||
            cadet.getFlagStatus().equals("RED")) {
            result.put("success", false);
            result.put("message", "Cadet is already at " +
                cadet.getFlagStatus() + " stage.");
            return result;
        }

        // Apply Yellow Flag
        cadet.setFlagStatus("YELLOW");
        cadet.setDaysSinceFlag(0);
        cadetRepo.save(cadet);

        // Record the flag event
        FlagEvent flagEvent = new FlagEvent(
            cadetId,
            cadet.getFullName(),
            "YELLOW",
            "Yellow Flag issued: " + reason,
            triggeredBy,
            LocalDate.now(),
            "Cadet has 5 days to re-engage before escalation to Orange."
        );
        flagEventRepo.save(flagEvent);

        // Record the notice sent event
        FlagEvent noticeEvent = new FlagEvent(
            cadetId,
            cadet.getFullName(),
            "NOTICE_SENT",
            "Formal written notice issued to cadet.",
            triggeredBy,
            LocalDate.now(),
            null
        );
        flagEventRepo.save(noticeEvent);

        result.put("success", true);
        result.put("message", "Yellow Flag issued. Cadet has 5 days to re-engage.");
        result.put("cadet", cadet);
        return result;
    }

    /**
     * STAGE 2 — Escalate to Orange Flag.
     *
     * Trigger: No response or re-engagement within 5 days
     *          of the Yellow Flag notice.
     *
     * Action:  Cadet removed from project or task team.
     *          Matter referred to Youth Academy Committee and Disciplinary Committee.
     *          Re-engagement plan to be agreed and documented.
     */

    public Map<String, Object> escalateToOrange(Long cadetId,
                                                 String triggeredBy) {
        Map<String, Object> result = new HashMap<>();

        Optional<Cadet> optional = cadetRepo.findById(cadetId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cadet not found.");
            return result;
        }

        Cadet cadet = optional.get();

        // Can only escalate from Yellow
        if (!cadet.getFlagStatus().equals("YELLOW")) {
            result.put("success", false);
            result.put("message", "Cadet must be at Yellow stage to escalate to Orange.");
            return result;
        }

        // Increment Orange flag count — used to detect RED threshold
        cadet.setOrangeFlagCount(cadet.getOrangeFlagCount() + 1);
        cadet.setFlagStatus("ORANGE");
        cadet.setDaysSinceFlag(0);
        cadetRepo.save(cadet);

        // Record Orange Flag event
        FlagEvent event = new FlagEvent(
            cadetId,
            cadet.getFullName(),
            "ORANGE",
            "Escalated to Orange Flag. Cadet removed from project team.",
            triggeredBy,
            LocalDate.now(),
            "Referred to YAC OR DC " +
            "Re-engagement plan must be agreed and documented."
        );
        flagEventRepo.save(event);

        // Check if this Orange flag triggers an automatic RED
        // Rule: 2 or more Orange flags within 12 months = RED
        LocalDate oneYearAgo = LocalDate.now().minusMonths(12);
        long orangeCount = flagEventRepo
            .countByCadetIdAndEventTypeAndEventDateBetween(
                cadetId, "ORANGE", oneYearAgo, LocalDate.now()
            );

        if (orangeCount >= 2) {
            return escalateToRed(cadetId,
                "System (auto-escalation)",
                "Two or more Orange flags issued within 12 months.");
        }

        result.put("success", true);
        result.put("message", "Escalated to Orange. Cadet removed from team. " +
            "Refer to Head of Mentorship.");
        result.put("cadet", cadet);
        return result;
    }

    /**
     * STAGE 3 — Escalate to Red Flag.
     *
     * Trigger: Two or more Orange flags within 12 months,
     *          OR failure to follow the agreed re-engagement plan.
     *
     * Action:  Matter referred to /Cadet Disciplinary
     *          committee OR Youth Academy Committee for formal review.
     *          Cadet ineligible for new project or leadership
     *          appointments until correction is completed.
     */
    public Map<String, Object> escalateToRed(Long cadetId,
                                              String triggeredBy,
                                              String reason) {
        Map<String, Object> result = new HashMap<>();

        Optional<Cadet> optional = cadetRepo.findById(cadetId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cadet not found.");
            return result;
        }

        Cadet cadet = optional.get();

        cadet.setFlagStatus("RED");
        cadet.setUnderCorrection(true);
        cadet.setDaysSinceFlag(0);
        cadetRepo.save(cadet);

        FlagEvent event = new FlagEvent(
            cadetId,
            cadet.getFullName(),
            "RED",
            "Red Flag issued: " + reason,
            triggeredBy,
            LocalDate.now(),
            "Referred to Disciplinary Committee and Youth Academy Committee. " +
            "Cadet is ineligible to serve in uniform " +
            "until correction is completed and status is restored."
        );
        flagEventRepo.save(event);

        result.put("success", true);
        result.put("message", "Red Flag issued. Referred to Disciplinary committee.");
        result.put("cadet", cadet);
        return result;
    }

    /**
     * RESTORATION — Remove correction and restore cadet status.
     *
     * Trigger: Correction completed to the satisfaction of the
     *         Youth Academy Committee and Disciplinary Committee.
     *
     * Action:  Flag cleared. underCorrection set to false.
     *          Record updated. Cadet eligible for appointments again.
     */
    public Map<String, Object> restoreCadet(Long cadetId,
                                             String triggeredBy,
                                             String notes) {
        Map<String, Object> result = new HashMap<>();

        Optional<Cadet> optional = cadetRepo.findById(cadetId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cadet not found.");
            return result;
        }

        Cadet cadet = optional.get();

        if (!cadet.getFlagStatus().equals("RED")) {
            result.put("success", false);
            result.put("message", "Only cadets at Red Flag stage can be formally restored.");
            return result;
        }

        cadet.setFlagStatus("NONE");
        cadet.setUnderCorrection(false);
        cadet.setDaysSinceFlag(0);
        cadet.setReengagementPlan("");
        cadetRepo.save(cadet);

        FlagEvent event = new FlagEvent(
            cadetId,
            cadet.getFullName(),
            "RESTORED",
            "Cadet status formally restored. Correction completed.",
            triggeredBy,
            LocalDate.now(),
            notes
        );
        flagEventRepo.save(event);

        result.put("success", true);
        result.put("message", "Cadet status restored. Record updated.");
        result.put("cadet", cadet);
        return result;
    }

    /**
     * PLAN AGREED — Document a re-engagement plan at Orange stage.
     *
     * Called after mentor meeting at the Orange stage.
     * Records the plan details as a FlagEvent and saves
     * the notes against the cadet profile.
     */
    public Map<String, Object> recordReengagementPlan(Long cadetId,
                                                       String triggeredBy,
                                                       String planDetails) {
        Map<String, Object> result = new HashMap<>();

        Optional<Cadet> optional = cadetRepo.findById(cadetId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cadet not found.");
            return result;
        }

        Cadet cadet = optional.get();
        cadet.setReengagementPlan(planDetails);
        cadetRepo.save(cadet);

        FlagEvent event = new FlagEvent(
            cadetId,
            cadet.getFullName(),
            "PLAN_AGREED",
            "Re-engagement plan agreed and documented.",
            triggeredBy,
            LocalDate.now(),
            planDetails
        );
        flagEventRepo.save(event);

        result.put("success", true);
        result.put("message", "Re-engagement plan recorded.");
        result.put("cadet", cadet);
        return result;
    }

    // ── Dashboard Metrics ─────────────────────────────────────

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalCadets", cadetRepo.count());
        metrics.put("activeCount", cadetRepo.countByFlagStatus("NONE"));
        metrics.put("yellowCount", cadetRepo.countByFlagStatus("YELLOW"));
        metrics.put("orangeCount", cadetRepo.countByFlagStatus("ORANGE"));
        metrics.put("redCount", cadetRepo.countByFlagStatus("RED"));
        metrics.put("underCorrection",
            cadetRepo.findByUnderCorrectionTrue().size());

        Double avgAtt = cadetRepo.findAverageAttendance();
        metrics.put("averageAttendance",
            avgAtt != null ? Math.round(avgAtt) : 0);

        List<Object[]> byProject = cadetRepo.findAverageAttendanceByProject();
        Map<String, Integer> projectAttendance = new HashMap<>();
        for (Object[] row : byProject) {
            String project = (String) row[0];
            Double avg = (Double) row[1];
            projectAttendance.put(project, avg != null ? (int) Math.round(avg) : 0);
        }
        metrics.put("attendanceByProject", projectAttendance);
        metrics.put("recentActivity", getRecentActivity());

        return metrics;
    }
}