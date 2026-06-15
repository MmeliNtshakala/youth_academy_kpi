package com.trsh.kpi.controller;

import com.trsh.kpi.model.*;
import com.trsh.kpi.repository.AppUserRepository;
import com.trsh.kpi.service.CadetProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CadetProfileController {

    private final CadetProfileService profileService;
    private final AppUserRepository userRepo;

    public CadetProfileController(
            CadetProfileService profileService,
            AppUserRepository userRepo) {
        this.profileService = profileService;
        this.userRepo       = userRepo;
    }

    // ── Helper — get current user ─────────────────────────────

    private AppUser getCurrentUser(Authentication auth) {
        if (auth == null) return null;
        return userRepo.findByUsername(auth.getName()).orElse(null);
    }

    // ── Cadet Profiles ────────────────────────────────────────

    /**
     * GET /api/cadets/{id}/profile
     * Returns the full profile for a cadet including
     * latest entry, full history, badges and projects.
     */
    @GetMapping("/cadets/{id}/profile")
    public ResponseEntity<Map<String, Object>> getCadetProfile(
            @PathVariable Long id) {

        Map<String, Object> result =
            profileService.getCadetFullProfile(id);

        if (Boolean.FALSE.equals(result.get("success"))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/cadets/{id}/profile
     * Save a new profile entry after a Liaison meeting.
     *
     * Request body (JSON):
     * {
     *   "wellbeing": "GREEN",
     *   "goals": "Complete leadership module by June",
     *   "challenges": "Struggling with time management",
     *   "personalNotes": "Cadet seems motivated but overwhelmed",
     *   "pressingInfo": "",
     *   "nextMeetingDate": "2026-06-15",
     *   "recordedBy": "Ms Dlamini"
     * }
     */
    @PostMapping("/cadets/{id}/profile")
    public ResponseEntity<Map<String, Object>> saveProfile(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        AppUser user = getCurrentUser(auth);
        String recordedBy = body.getOrDefault(
            "recordedBy",
            user != null ? user.getFullName() : "Liaison"
        );

        String wellbeing      = body.getOrDefault("wellbeing", "GREEN");
        String goals          = body.getOrDefault("goals", "");
        String challenges     = body.getOrDefault("challenges", "");
        String personalNotes  = body.getOrDefault("personalNotes", "");
        String pressingInfo   = body.getOrDefault("pressingInfo", "");
        String nextDateStr    = body.getOrDefault("nextMeetingDate", "");

        LocalDate nextMeetingDate = null;
        if (!nextDateStr.isBlank()) {
            try { nextMeetingDate = LocalDate.parse(nextDateStr); }
            catch (Exception ignored) {}
        }

        Map<String, Object> result = profileService.saveProfile(
            id, wellbeing, goals, challenges,
            personalNotes, pressingInfo,
            nextMeetingDate, recordedBy
        );

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/profiles/wellbeing
     * Returns wellbeing summary — GREEN/AMBER/RED counts.
     * Used for dashboard widget.
     * Liaisons see their region only.
     */
    @GetMapping("/profiles/wellbeing")
    public ResponseEntity<Map<String, Object>> getWellbeing(
            Authentication auth) {

        AppUser user = getCurrentUser(auth);
        String region = (user != null && !user.isAdmin())
            ? user.getRegion() : null;

        return ResponseEntity.ok(
            profileService.getWellbeingSummary(region)
        );
    }

    /**
     * GET /api/profiles/pressing
     * Returns all cadet profiles with pressing info.
     * Used to alert Liaisons of urgent matters.
     */
    @GetMapping("/profiles/pressing")
    public ResponseEntity<List<CadetProfile>> getPressing(
            Authentication auth) {

        AppUser user = getCurrentUser(auth);
        String region = (user != null && !user.isAdmin())
            ? user.getRegion() : null;

        return ResponseEntity.ok(
            profileService.getPressingProfiles(region)
        );
    }

    // ── Projects ──────────────────────────────────────────────

    /**
     * GET /api/projects
     * Returns all projects visible to the logged-in user.
     * Admin sees all. Liaison sees national + their region.
     */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectEntity>> getProjects(
            Authentication auth) {

        AppUser user = getCurrentUser(auth);
        String role   = user != null ? user.getRole()   : "LIAISON";
        String region = user != null ? user.getRegion() : "ALL";

        return ResponseEntity.ok(
            profileService.getProjectsForUser(role, region)
        );
    }

    /**
     * GET /api/projects/{id}
     * Returns a single project with milestones,
     * assigned cadets and badge count.
     */
    @GetMapping("/projects/{id}")
    public ResponseEntity<Map<String, Object>> getProject(
            @PathVariable Long id) {

        Map<String, Object> result =
            profileService.getProjectDetail(id);

        if (Boolean.FALSE.equals(result.get("success"))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/projects
     * Create a new project with milestones.
     *
     * Request body (JSON):
     * {
     *   "name": "Youth Leadership Summit 2026",
     *   "description": "Annual leadership development programme",
     *   "scope": "NATIONAL",
     *   "region": "ALL",
     *   "startDate": "2026-05-01",
     *   "targetEndDate": "2026-11-30",
     *   "badgeName": "Youth Leadership Summit 2026",
     *   "milestones": [
     *     { "title": "Planning & Setup",
     *       "description": "Secure venue and resources",
     *       "targetDate": "2026-06-01" },
     *     { "title": "Recruitment",
     *       "description": "Enrol participants",
     *       "targetDate": "2026-07-01" },
     *     { "title": "Training Sessions",
     *       "description": "Run all training modules",
     *       "targetDate": "2026-10-01" },
     *     { "title": "Final Event",
     *       "description": "Host the summit",
     *       "targetDate": "2026-11-30" }
     *   ]
     * }
     */
    @PostMapping("/projects")
    public ResponseEntity<Map<String, Object>> createProject(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        AppUser user = getCurrentUser(auth);

        String name        = (String) body.getOrDefault("name", "");
        String description = (String) body.getOrDefault(
            "description", "");
        String badgeName   = (String) body.getOrDefault(
            "badgeName", name);

        // Admin creates NATIONAL, Liaison creates PROVINCIAL
        String scope = user != null && user.isAdmin()
            ? (String) body.getOrDefault("scope", "NATIONAL")
            : "PROVINCIAL";

        String region = user != null && !user.isAdmin()
            ? user.getRegion()
            : (String) body.getOrDefault("region", "ALL");

        String createdBy = user != null
            ? user.getFullName() : "Admin";

        String startStr  =
            (String) body.getOrDefault("startDate", "");
        String endStr    =
            (String) body.getOrDefault("targetEndDate", "");

        LocalDate startDate = null, targetEndDate = null;
        try {
            startDate = startStr.isBlank()
                ? LocalDate.now() : LocalDate.parse(startStr);
            targetEndDate = endStr.isBlank()
                ? null : LocalDate.parse(endStr);
        } catch (Exception ignored) {}

        @SuppressWarnings("unchecked")
        List<Map<String, String>> milestones =
            (List<Map<String, String>>)
                body.getOrDefault("milestones", List.of());

        Map<String, Object> result = profileService.createProject(
            name, description, scope, region,
            createdBy, startDate, targetEndDate,
            badgeName, milestones
        );

        return ResponseEntity.ok(result);
    }

    // ── Milestones ────────────────────────────────────────────

    /**
     * POST /api/milestones/{id}/complete
     * Mark a milestone as complete.
     * Auto-awards badges if project hits 100%.
     *
     * Request body:
     * { "completedBy": "Ms Dlamini" }
     */
    @PostMapping("/milestones/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeMilestone(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        AppUser user = getCurrentUser(auth);
        String completedBy = body.getOrDefault(
            "completedBy",
            user != null ? user.getFullName() : "Admin"
        );

        return ResponseEntity.ok(
            profileService.completeMilestone(id, completedBy)
        );
    }

    /**
     * POST /api/milestones/{id}/uncomplete
     * Undo a milestone completion.
     */
    @PostMapping("/milestones/{id}/uncomplete")
    public ResponseEntity<Map<String, Object>> uncompleteMilestone(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            profileService.uncompleteMilestone(id)
        );
    }

    // ── Cadet project assignment ──────────────────────────────

    /**
     * POST /api/projects/{id}/assign
     * Assign a cadet to a project.
     *
     * Request body:
     * { "cadetId": 1 }
     */
    @PostMapping("/projects/{id}/assign")
    public ResponseEntity<Map<String, Object>> assignCadet(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        AppUser user = getCurrentUser(auth);
        Long cadetId = Long.valueOf(
            body.get("cadetId").toString()
        );
        String assignedBy = user != null
            ? user.getFullName() : "Admin";

        return ResponseEntity.ok(
            profileService.assignCadet(cadetId, id, assignedBy)
        );
    }

    /**
     * POST /api/projects/{id}/remove
     * Remove a cadet from a project.
     *
     * Request body:
     * { "cadetId": 1 }
     */
    @PostMapping("/projects/{id}/remove")
    public ResponseEntity<Map<String, Object>> removeCadet(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Long cadetId = Long.valueOf(
            body.get("cadetId").toString()
        );

        return ResponseEntity.ok(
            profileService.removeCadet(cadetId, id)
        );
    }
}