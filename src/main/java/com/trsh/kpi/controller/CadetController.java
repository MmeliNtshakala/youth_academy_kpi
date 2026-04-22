package com.trsh.kpi.controller;

import com.trsh.kpi.model.Cadet;
import com.trsh.kpi.model.FlagEvent;
import com.trsh.kpi.service.CadetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class CadetController {

    private final CadetService cadetService;

    public CadetController(CadetService cadetService) {
        this.cadetService = cadetService;
    }

    // ── Dashboard ─────────────────────────────────────────────

    /**
     * GET /api/dashboard
     * Returns all KPI metrics for the dashboard:
     * counts, averages, attendance by project, recent activity.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(cadetService.getDashboardMetrics());
    }

    // ── Cadets ────────────────────────────────────────────────

    /**
     * GET /api/cadets
     * Returns all cadets. Supports optional filters:
     * ?flag=YELLOW  → filter by flag status
     * ?project=Leadership → filter by project
     * ?search=Thabo → search by name
     */
    @GetMapping("/cadets")
    public ResponseEntity<List<Cadet>> getCadets(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String search) {

        List<Cadet> cadets;

        if (search != null && !search.isBlank()) {
            cadets = cadetService.searchCadets(search);
        } else if (flag != null && !flag.isBlank()) {
            cadets = cadetService.getCadetsByFlag(flag.toUpperCase());
        } else if (project != null && !project.isBlank()) {
            cadets = cadetService.getCadetsByProject(project);
        } else {
            cadets = cadetService.getAllCadets();
        }

        return ResponseEntity.ok(cadets);
    }

    /**
     * GET /api/cadets/{id}
     * Returns a single cadet by their database ID.
     */
    @GetMapping("/cadets/{id}")
    public ResponseEntity<Cadet> getCadetById(@PathVariable Long id) {
        Optional<Cadet> cadet = cadetService.getCadetById(id);
        return cadet.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/cadets
     * Registers a new cadet.
     *
     * Request body (JSON):
     * {
     *   "cadetCode": "CDT-0301",
     *   "fullName": "Thabo Nkosi",
     *   "Region": "Gauteng",
     *   "flagStatus": "NONE",
     *   "attendancePercent": 80,
     *   "projectManager": "Liason name",
     *   "lastContactDate": "2026-04-16"
     * }
     */
    @PostMapping("/cadets")
    public ResponseEntity<Cadet> createCadet(@RequestBody Cadet cadet) {
        if (cadet.getFlagStatus() == null || cadet.getFlagStatus().isBlank()) {
            cadet.setFlagStatus("NONE");
        }
        Cadet saved = cadetService.saveCadet(cadet);
        return ResponseEntity.ok(saved);
    }

    /**
     * PUT /api/cadets/{id}
     * Updates an existing cadet's details.
     * Used for editing attendance, project, PM etc.
     */
    @PutMapping("/cadets/{id}")
    public ResponseEntity<Cadet> updateCadet(@PathVariable Long id,
                                              @RequestBody Cadet updated) {
        Optional<Cadet> optional = cadetService.getCadetById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Cadet cadet = optional.get();
        cadet.setFullName(updated.getFullName());
        cadet.setProject(updated.getProject());
        cadet.setProjectManager(updated.getProjectManager());
        cadet.setAttendancePercent(updated.getAttendancePercent());
        cadet.setLastContactDate(updated.getLastContactDate());
        cadet.setReengagementPlan(updated.getReengagementPlan());

        return ResponseEntity.ok(cadetService.saveCadet(cadet));
    }

    /**
     * DELETE /api/cadets/{id}
     * Removes a cadet from the system.
     */
    @DeleteMapping("/cadets/{id}")
    public ResponseEntity<Void> deleteCadet(@PathVariable Long id) {
        cadetService.deleteCadet(id);
        return ResponseEntity.noContent().build();
    }

    // ── Flag Actions ──────────────────────────────────────────

    /**
     * POST /api/cadets/{id}/flag/yellow
     * Issues a Yellow Flag against a cadet.
     *
     * Request body (JSON):
     * {
     *   "triggeredBy": "Ms Dlamini",
     *   "reason": "Missed 2 consecutive meetings"
     * }
     */
    @PostMapping("/cadets/{id}/flag/yellow")
    public ResponseEntity<Map<String, Object>> issueYellowFlag(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String triggeredBy = body.getOrDefault("triggeredBy", "System");
        String reason = body.getOrDefault("reason",
            "Missed 2 consecutive meetings or no response for 14 days.");

        return ResponseEntity.ok(
            cadetService.issueYellowFlag(id, triggeredBy, reason)
        );
    }

    /**
     * POST /api/cadets/{id}/flag/orange
     * Escalates a cadet from Yellow to Orange.
     * Auto-escalates to Red if 2+ Orange flags in 12 months.
     *
     * Request body (JSON):
     * {
     *   "triggeredBy": "Ms Dlamini"
     * }
     */
    @PostMapping("/cadets/{id}/flag/orange")
    public ResponseEntity<Map<String, Object>> escalateToOrange(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String triggeredBy = body.getOrDefault("triggeredBy", "System");

        return ResponseEntity.ok(
            cadetService.escalateToOrange(id, triggeredBy)
        );
    }

    /**
     * POST /api/cadets/{id}/flag/red
     * Manually escalates a cadet to Red Flag.
     * Used when re-engagement plan has failed.
     *
     * Request body (JSON):
     * {
     *   "triggeredBy": "Ms Dlamini",
     *   "reason": "Failed to follow agreed re-engagement plan"
     * }
     */
    @PostMapping("/cadets/{id}/flag/red")
    public ResponseEntity<Map<String, Object>> escalateToRed(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String triggeredBy = body.getOrDefault("triggeredBy", "System");
        String reason = body.getOrDefault("reason",
            "Failed to follow agreed re-engagement plan.");

        return ResponseEntity.ok(
            cadetService.escalateToRed(id, triggeredBy, reason)
        );
    }

    /**
     * POST /api/cadets/{id}/restore
     * Restores a cadet's status after correction is completed.
     *
     * Request body (JSON):
     * {
     *   "triggeredBy": "Liaison",
     *   "notes": "Correction completed satisfactorily."
     * }
     */
    @PostMapping("/cadets/{id}/restore")
    public ResponseEntity<Map<String, Object>> restoreCadet(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String triggeredBy = body.getOrDefault("triggeredBy", "System");
        String notes = body.getOrDefault("notes", "");

        return ResponseEntity.ok(
            cadetService.restoreCadet(id, triggeredBy, notes)
        );
    }

    /**
     * POST /api/cadets/{id}/plan
     * Records a re-engagement plan agreed at the Orange stage.
     *
     * Request body (JSON):
     * {
     *   "triggeredBy": "Liaison",
     *   "planDetails": "Cadet to attend weekly check-ins for 4 weeks."
     * }
     */
    @PostMapping("/cadets/{id}/plan")
    public ResponseEntity<Map<String, Object>> recordPlan(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String triggeredBy = body.getOrDefault("triggeredBy", "System");
        String planDetails = body.getOrDefault("planDetails", "");

        return ResponseEntity.ok(
            cadetService.recordReengagementPlan(id, triggeredBy, planDetails)
        );
    }

    // ── Flag History ──────────────────────────────────────────

    /**
     * GET /api/cadets/{id}/history
     * Returns the full flag event history for a cadet,
     * newest first.
     */
    @GetMapping("/cadets/{id}/history")
    public ResponseEntity<List<FlagEvent>> getFlagHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(cadetService.getFlagHistory(id));
    }
}