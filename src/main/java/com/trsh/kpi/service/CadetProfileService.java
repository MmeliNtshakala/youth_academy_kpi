package com.trsh.kpi.service;

import com.trsh.kpi.model.*;
import com.trsh.kpi.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class CadetProfileService {

    private final CadetProfileRepository profileRepo;
    private final ProjectRepository projectRepo;
    private final MilestoneRepository milestoneRepo;
    private final CadetProjectRepository cadetProjectRepo;
    private final CadetRepository cadetRepo;

    public CadetProfileService(
            CadetProfileRepository profileRepo,
            ProjectRepository projectRepo,
            MilestoneRepository milestoneRepo,
            CadetProjectRepository cadetProjectRepo,
            CadetRepository cadetRepo) {
        this.profileRepo      = profileRepo;
        this.projectRepo      = projectRepo;
        this.milestoneRepo    = milestoneRepo;
        this.cadetProjectRepo = cadetProjectRepo;
        this.cadetRepo        = cadetRepo;
    }

    // ── Cadet Profiles ────────────────────────────────────────

    /**
     * Save a new profile entry for a cadet.
     * Called after every Liaison meeting.
     * A new record is always created — history is preserved.
     */
    public Map<String, Object> saveProfile(
            Long cadetId, String wellbeing,
            String goals, String challenges,
            String personalNotes, String pressingInfo,
            LocalDate nextMeetingDate, String recordedBy) {

        Map<String, Object> result = new HashMap<>();

        Optional<Cadet> cadetOpt = cadetRepo.findById(cadetId);
        if (cadetOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cadet not found.");
            return result;
        }

        Cadet cadet = cadetOpt.get();

        // Validate wellbeing value
        if (!List.of("GREEN","AMBER","RED").contains(wellbeing)) {
            result.put("success", false);
            result.put("message",
                "Wellbeing must be GREEN, AMBER or RED.");
            return result;
        }

        CadetProfile profile = new CadetProfile(
            cadetId,
            cadet.getFullName(),
            cadet.getCadetCode(),
            cadet.getProject(),
            wellbeing,
            goals,
            challenges,
            personalNotes,
            pressingInfo,
            nextMeetingDate,
            recordedBy
        );

        profileRepo.save(profile);

        result.put("success", true);
        result.put("message", "Profile saved successfully.");
        result.put("profile", profile);
        return result;
    }

    /**
     * Get the full profile for a cadet.
     * Returns latest entry + full history + badges + projects.
     */
    public Map<String, Object> getCadetFullProfile(Long cadetId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Cadet> cadetOpt = cadetRepo.findById(cadetId);
        if (cadetOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cadet not found.");
            return result;
        }

        Cadet cadet = cadetOpt.get();

        // Latest profile entry
        Optional<CadetProfile> latest =
            profileRepo.findFirstByCadetIdOrderByRecordedDateDesc(
                cadetId
            );

        // Full profile history
        List<CadetProfile> history =
            profileRepo.findByCadetIdOrderByRecordedDateDesc(cadetId);

        // All badges earned
        List<CadetProject> badges =
            cadetProjectRepo.findByCadetIdAndBadgeEarned(
                cadetId, true
            );

        // All projects assigned
        List<CadetProject> projects =
            cadetProjectRepo.findByCadetId(cadetId);

        result.put("success",  true);
        result.put("cadet",    cadet);
        result.put("latest",   latest.orElse(null));
        result.put("history",  history);
        result.put("badges",   badges);
        result.put("projects", projects);
        return result;
    }

    /**
     * Get all profiles with pressing info.
     * Used to show Liaison urgent matters on dashboard.
     */
    public List<CadetProfile> getPressingProfiles(String region) {
        return profileRepo.findByPressingInfoIsNotNull()
            .stream()
            .filter(p -> !p.getPressingInfo().isBlank())
            .filter(p -> region == null ||
                         p.getRegion().equals(region))
            .toList();
    }

    /**
     * Get wellbeing summary for dashboard.
     * Returns count of GREEN, AMBER, RED cadets.
     * Only counts the most recent profile per cadet.
     */
    public Map<String, Object> getWellbeingSummary(String region) {
        Map<String, Object> result = new HashMap<>();

        List<Cadet> cadets = region != null
            ? cadetRepo.findByProject(region)
            : cadetRepo.findAll();

        int green = 0, amber = 0, red = 0, noProfile = 0;

        for (Cadet cadet : cadets) {
            Optional<CadetProfile> latest =
                profileRepo
                    .findFirstByCadetIdOrderByRecordedDateDesc(
                        cadet.getId()
                    );
            if (latest.isEmpty()) {
                noProfile++;
            } else {
                switch (latest.get().getWellbeing()) {
                    case "GREEN" -> green++;
                    case "AMBER" -> amber++;
                    case "RED"   -> red++;
                }
            }
        }

        result.put("green",     green);
        result.put("amber",     amber);
        result.put("red",       red);
        result.put("noProfile", noProfile);
        result.put("total",     cadets.size());
        return result;
    }

    // ── Projects ──────────────────────────────────────────────

    /**
     * Create a new project.
     * NATIONAL — Admin only, visible to all regions.
     * PROVINCIAL — Liaison, visible in their region only.
     */
    public Map<String, Object> createProject(
            String name, String description,
            String scope, String region,
            String createdBy, LocalDate startDate,
            LocalDate targetEndDate, String badgeName,
            List<Map<String, String>> milestones) {

        Map<String, Object> result = new HashMap<>();

        if (name == null || name.isBlank()) {
            result.put("success", false);
            result.put("message", "Project name is required.");
            return result;
        }

        if (milestones == null || milestones.isEmpty()) {
            result.put("success", false);
            result.put("message",
                "At least one milestone is required.");
            return result;
        }

        ProjectEntity project = new ProjectEntity(
            name, description, scope, region,
            createdBy, startDate, targetEndDate, badgeName
        );

        ProjectEntity saved = projectRepo.save(project);

        // Create milestones
        List<Milestone> savedMilestones = new ArrayList<>();
        for (int i = 0; i < milestones.size(); i++) {
            Map<String, String> m = milestones.get(i);
            String mTitle = m.getOrDefault("title", "Milestone " + (i+1));
            String mDesc  = m.getOrDefault("description", "");
            String mDate  = m.getOrDefault("targetDate", "");

            LocalDate targetDate = null;
            if (!mDate.isBlank()) {
                try { targetDate = LocalDate.parse(mDate); }
                catch (Exception ignored) {}
            }

            Milestone milestone = new Milestone(
                saved, mTitle, mDesc, i + 1, targetDate
            );
            savedMilestones.add(milestoneRepo.save(milestone));
        }

        result.put("success",    true);
        result.put("message",    "Project created successfully.");
        result.put("project",    saved);
        result.put("milestones", savedMilestones);
        return result;
    }

    /**
     * Get all projects visible to a user.
     * Admin sees all. Liaison sees national + their region.
     */
    public List<ProjectEntity> getProjectsForUser(
            String role, String region) {
        if ("ADMIN".equals(role)) {
            return projectRepo.findAll();
        }
        // Liaison sees national + their provincial projects
        return projectRepo.findByScopeOrRegion("NATIONAL", region);
    }

    /**
     * Get a single project with its milestones.
     */
    public Map<String, Object> getProjectDetail(Long projectId) {
        Map<String, Object> result = new HashMap<>();

        Optional<ProjectEntity> opt =
            projectRepo.findById(projectId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Project not found.");
            return result;
        }

        ProjectEntity project = opt.get();
        List<Milestone> milestones =
            milestoneRepo
                .findByProjectIdOrderByMilestoneOrderAsc(projectId);
        List<CadetProject> assigned =
            cadetProjectRepo.findByProjectId(projectId);

        long badgeCount =
            cadetProjectRepo.countByProjectIdAndBadgeEarned(
                projectId, true
            );

        result.put("success",    true);
        result.put("project",    project);
        result.put("milestones", milestones);
        result.put("assigned",   assigned);
        result.put("badgeCount", badgeCount);
        return result;
    }

    // ── Milestones ────────────────────────────────────────────

    /**
     * Mark a milestone as complete.
     * Recalculates project progress automatically.
     * Awards badges if project hits 100%.
     */
    public Map<String, Object> completeMilestone(
            Long milestoneId, String completedBy) {

        Map<String, Object> result = new HashMap<>();

        Optional<Milestone> opt =
            milestoneRepo.findById(milestoneId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Milestone not found.");
            return result;
        }

        Milestone milestone = opt.get();
        if (milestone.isCompleted()) {
            result.put("success", false);
            result.put("message", "Milestone already completed.");
            return result;
        }

        // Mark complete — triggers progress recalculation
        milestone.markComplete(completedBy);
        milestoneRepo.save(milestone);

        // Save updated project progress
        ProjectEntity project = milestone.getProject();
        projectRepo.save(project);

        result.put("success",  true);
        result.put("progress", project.getProgressPercent());
        result.put("status",   project.getStatus());

        // ── AUTO BADGE AWARD ──────────────────────────────────
        // If project just hit 100% award badges to all
        // assigned cadets who haven't received one yet.

        if (project.isCompleted() && !project.isBadgeAwarded()) {
            List<String> badgedCadets =
                awardBadgesToProject(project);
            result.put("badgesAwarded", badgedCadets.size());
            result.put("badgedCadets",  badgedCadets);
            result.put("badgeNotice",
                "Project complete! Badge ⭐ " +
                project.getBadgeName() +
                " awarded to Cadet" + badgedCadets.size() +
                " Well Done.");
        }

        return result;
    }

    /**
     * Undo a milestone completion.
     */
    public Map<String, Object> uncompleteMilestone(
            Long milestoneId) {

        Map<String, Object> result = new HashMap<>();

        Optional<Milestone> opt =
            milestoneRepo.findById(milestoneId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Milestone not found.");
            return result;
        }

        Milestone milestone = opt.get();
        milestone.markIncomplete();
        milestoneRepo.save(milestone);

        ProjectEntity project = milestone.getProject();

        // Revert status if it was completed
        if ("COMPLETED".equals(project.getStatus())) {
            project.setStatus("ACTIVE");
        }
        projectRepo.save(project);

        result.put("success",  true);
        result.put("progress", project.getProgressPercent());
        return result;
    }

    // ── Badge awarding ────────────────────────────────────────

    /**
     * Award badges to all active cadets on a completed project.
     * Returns list of cadet names who received the badge.
     */
    private List<String> awardBadgesToProject(
            ProjectEntity project) {

        List<CadetProject> unbadged =
            cadetProjectRepo.findUnbadgedCadets(project.getId());

        List<String> badgedNames = new ArrayList<>();

        for (CadetProject cp : unbadged) {
            cp.awardBadge(project.getBadgeName());
            cadetProjectRepo.save(cp);
            badgedNames.add(cp.getCadetName());
        }

        // Mark project badges as awarded
        project.setBadgeAwarded(true);
        projectRepo.save(project);

        return badgedNames;
    }

    // ── Cadet project assignment ──────────────────────────────

    /**
     * Assign a cadet to a project.
     */
    public Map<String, Object> assignCadet(
            Long cadetId, Long projectId, String assignedBy) {

        Map<String, Object> result = new HashMap<>();

        // Check cadet exists
        Optional<Cadet> cadetOpt = cadetRepo.findById(cadetId);
        if (cadetOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cadet not found.");
            return result;
        }

        // Check project exists
        Optional<ProjectEntity> projectOpt =
            projectRepo.findById(projectId);
        if (projectOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Project not found.");
            return result;
        }

        // Check not already assigned
        if (cadetProjectRepo.existsByCadetIdAndProjectId(
                cadetId, projectId)) {
            result.put("success", false);
            result.put("message", "Cadet already assigned.");
            return result;
        }

        Cadet cadet           = cadetOpt.get();
        ProjectEntity project = projectOpt.get();

        CadetProject cp = new CadetProject(
            cadetId,
            cadet.getFullName(),
            cadet.getCadetCode(),
            cadet.getProject(),
            projectId,
            project.getName(),
            project.getScope(),
            assignedBy
        );

        cadetProjectRepo.save(cp);

        result.put("success", true);
        result.put("message",
            cadet.getFullName() +
            " Cadet assigned to " + project.getName());
        return result;
    }

    /**
     * Remove a cadet from a project.
     */
    public Map<String, Object> removeCadet(
            Long cadetId, Long projectId) {

        Map<String, Object> result = new HashMap<>();

        Optional<CadetProject> cp =
            cadetProjectRepo.findByCadetIdAndProjectId(
                cadetId, projectId
            );

        if (cp.isEmpty()) {
            result.put("success", false);
            result.put("message", "Assignment not found.");
            return result;
        }

        cp.get().setParticipationStatus("WITHDRAWN");
        cadetProjectRepo.save(cp.get());

        result.put("success", true);
        result.put("message", "Cadet removed from project.");
        return result;
    }
}