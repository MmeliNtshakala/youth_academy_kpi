package com.trsh.kpi.controller;

import com.trsh.kpi.model.Meeting;
import com.trsh.kpi.service.MeetingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    // ── Meetings ──────────────────────────────────────────────

    /**
     * GET /api/meetings
     * Returns all meetings, newest first.
     * Optional filter: ?region=Gauteng
     */
    @GetMapping
    public ResponseEntity<List<Meeting>> getMeetings(
            @RequestParam(required = false) String region) {

        List<Meeting> meetings;
        if (region != null && !region.isBlank()) {
            meetings = meetingService.getMeetingsByRegion(region);
        } else {
            meetings = meetingService.getAllMeetings();
        }
        return ResponseEntity.ok(meetings);
    }

    /**
     * GET /api/meetings/recent
     * Returns the 5 most recent meetings.
     * Used for the register tab landing page.
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Meeting>> getRecentMeetings() {
        return ResponseEntity.ok(meetingService.getRecentMeetings());
    }

    /**
     * GET /api/meetings/incomplete
     * Returns all meetings where register is not yet complete.
     * Used to alert PMs of pending registers.
     */
    @GetMapping("/incomplete")
    public ResponseEntity<List<Meeting>> getIncomplete() {
        return ResponseEntity.ok(meetingService.getIncompleteRegisters());
    }

    /**
     * GET /api/meetings/{id}
     * Returns a single meeting by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Meeting> getMeetingById(
            @PathVariable Long id) {

        Optional<Meeting> meeting = meetingService.getMeetingById(id);
        return meeting.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/meetings
     * Creates a new meeting.
     *
     * Request body (JSON):
     * {
     *   "meetingType": "Sunday Session",
     *   "title": "Sunday Session — 20 Apr 2026",
     *   "region": "Gauteng",
     *   "meetingDate": "2026-04-20",
     *   "createdBy": "Ms Dlamini",
     *   "notes": "Monthly review session"
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMeeting(
            @RequestBody Map<String, String> body) {

        String meetingType  = body.getOrDefault("meetingType", "Sunday Session");
        String title        = body.getOrDefault("title", "");
        String region       = body.getOrDefault("region", "ALL");
        String meetingDate  = body.getOrDefault("meetingDate", "");
        String createdBy    = body.getOrDefault("createdBy", "Admin");
        String notes        = body.getOrDefault("notes", "");

        if (meetingDate.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("success", false,
                       "message", "Meeting date is required.")
            );
        }

        LocalDate date;
        try {
            date = LocalDate.parse(meetingDate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("success", false,
                       "message", "Invalid date format. Use YYYY-MM-DD.")
            );
        }

        Map<String, Object> result = meetingService.createMeeting(
            meetingType, title, region, date, createdBy, notes
        );
        return ResponseEntity.ok(result);
    }

    // ── Register ──────────────────────────────────────────────

    /**
     * GET /api/meetings/{id}/register
     * Loads the register sheet for a meeting.
     * Returns all cadets in the meeting's region,
     * grouped by province, with status pre-filled
     * if already recorded.
     */
    @GetMapping("/{id}/register")
    public ResponseEntity<Map<String, Object>> getRegister(
            @PathVariable Long id) {

        Map<String, Object> result = meetingService.getRegisterSheet(id);
        if (Boolean.FALSE.equals(result.get("success"))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/meetings/{id}/register
     * Submits the completed register for a meeting.
     *
     * Enforces:
     * - Every cadet must have a status
     * - Every absent cadet must have a reason
     * - Auto Yellow Flag if 2 consecutive absences
     *
     * Request body (JSON):
     * {
     *   "recordedBy": "Ms Dlamini",
     *   "entries": [
     *     {
     *       "cadetId": 1,
     *       "cadetCode": "CDT-0001",
     *       "fullName": "Thabo Nkosi",
     *       "region": "Gauteng",
     *       "status": "PRESENT",
     *       "absenceReason": ""
     *     },
     *     {
     *       "cadetId": 2,
     *       "cadetCode": "CDT-0002",
     *       "fullName": "Lerato Dlamini",
     *       "region": "Gauteng",
     *       "status": "ABSENT",
     *       "absenceReason": "Family emergency"
     *     }
     *   ]
     * }
     */
    @PostMapping("/{id}/register")
    public ResponseEntity<Map<String, Object>> submitRegister(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        String recordedBy = (String) body.getOrDefault(
            "recordedBy", "Admin"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries =
            (List<Map<String, Object>>) body.get("entries");

        if (entries == null || entries.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("success", false,
                       "message", "No attendance entries provided.")
            );
        }

        Map<String, Object> result =
            meetingService.submitRegister(id, entries, recordedBy);

        return ResponseEntity.ok(result);
    }

    // ── Reports ───────────────────────────────────────────────

    /**
     * GET /api/meetings/{id}/report
     * Returns the full attendance report for one meeting.
     * Shows present, absent, late counts grouped by region.
     */
    @GetMapping("/{id}/report")
    public ResponseEntity<Map<String, Object>> getMeetingReport(
            @PathVariable Long id) {

        Map<String, Object> result =
            meetingService.getMeetingAttendanceReport(id);

        if (Boolean.FALSE.equals(result.get("success"))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/meetings/region-summary
     * Returns attendance percentage per region.
     * Powers the attendance by region chart on the dashboard.
     */
    @GetMapping("/region-summary")
    public ResponseEntity<Map<String, Object>> getRegionSummary() {
        return ResponseEntity.ok(
            meetingService.getRegionAttendanceSummary()
        );
    }

    /**
     * GET /api/meetings/cadet/{cadetId}/report
     * Returns the full attendance history for one cadet.
     * Total meetings, attended, absences, and every record.
     */
    @GetMapping("/cadet/{cadetId}/report")
    public ResponseEntity<Map<String, Object>> getCadetReport(
            @PathVariable Long cadetId) {

        Map<String, Object> result =
            meetingService.getCadetAttendanceReport(cadetId);

        if (Boolean.FALSE.equals(result.get("success"))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
}