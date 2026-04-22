package com.trsh.kpi.service;

import com.trsh.kpi.model.AttendanceRecord;
import com.trsh.kpi.model.Cadet;
import com.trsh.kpi.model.Meeting;
import com.trsh.kpi.repository.AttendanceRepository;
import com.trsh.kpi.repository.CadetRepository;
import com.trsh.kpi.repository.MeetingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepo;
    private final AttendanceRepository attendanceRepo;
    private final CadetRepository cadetRepo;
    private final CadetService cadetService;

    public MeetingService(MeetingRepository meetingRepo,
                          AttendanceRepository attendanceRepo,
                          CadetRepository cadetRepo,
                          CadetService cadetService) {
        this.meetingRepo = meetingRepo;
        this.attendanceRepo = attendanceRepo;
        this.cadetRepo = cadetRepo;
        this.cadetService = cadetService;
    }

    // ── Meetings ──────────────────────────────────────────────

    public List<Meeting> getAllMeetings() {
        return meetingRepo.findAll();
    }

    public Optional<Meeting> getMeetingById(Long id) {
        return meetingRepo.findById(id);
    }

    public List<Meeting> getMeetingsByRegion(String region) {
        return meetingRepo.findByRegionOrderByMeetingDateDesc(region);
    }

    public List<Meeting> getRecentMeetings() {
        return meetingRepo.findTop5ByOrderByMeetingDateDesc();
    }

    public List<Meeting> getIncompleteRegisters() {
        return meetingRepo.findByRegisterCompleteFalse();
    }

    /**
     * Create a new meeting.
     * Meeting type is either "Sunday Session" or "Special Event".
     * Region can be a specific province or "ALL" for all regions.
     */
    public Map<String, Object> createMeeting(String meetingType,
                                              String title,
                                              String region,
                                              LocalDate meetingDate,
                                              String createdBy,
                                              String notes) {
        Map<String, Object> result = new HashMap<>();

        if (title == null || title.isBlank()) {
            title = meetingType + " — " +
                meetingDate.getDayOfMonth() + " " +
                meetingDate.getMonth().toString().charAt(0) +
                meetingDate.getMonth().toString()
                    .substring(1).toLowerCase() + " " +
                meetingDate.getYear();
        }

        Meeting meeting = new Meeting(
            meetingType, title, region,
            meetingDate, createdBy, notes
        );
        Meeting saved = meetingRepo.save(meeting);

        result.put("success", true);
        result.put("message", "Meeting created successfully.");
        result.put("meeting", saved);
        return result;
    }

    // ── Register ──────────────────────────────────────────────

    /**
     * Load the register sheet for a meeting.
     * Returns all cadets in the meeting's region,
     * grouped by region, with their current attendance
     * status pre-filled if already recorded.
     */
    public Map<String, Object> getRegisterSheet(Long meetingId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Meeting> optional = meetingRepo.findById(meetingId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "Meeting not found.");
            return result;
        }

        Meeting meeting = optional.get();

        // Get cadets for this region
        List<Cadet> cadets;
        if (meeting.getRegion().equals("ALL")) {
            cadets = cadetRepo.findAll();
        } else {
            cadets = cadetRepo.findByProject(meeting.getRegion());
        }

        // Get any existing attendance records for this meeting
        List<AttendanceRecord> existing =
            attendanceRepo.findByMeetingId(meetingId);
        Map<Long, AttendanceRecord> existingMap = new HashMap<>();
        for (AttendanceRecord r : existing) {
            existingMap.put(r.getCadetId(), r);
        }

        // Group cadets by region
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        String[] regionOrder = {
            "Gauteng", "eMazweni", "eMangalisweni", "eZenzweni", "Mozambique", "Zimbabwe", "Botswana", "International"
        };

        for (String region : regionOrder) {
            grouped.put(region, new ArrayList<>());
        }

        for (Cadet cadet : cadets) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("cadetId",   cadet.getId());
            entry.put("cadetCode", cadet.getCadetCode());
            entry.put("fullName",  cadet.getFullName());
            entry.put("region",    cadet.getProject());

            // Pre-fill if already recorded
            AttendanceRecord existing_ = existingMap.get(cadet.getId());
            if (existing_ != null) {
                entry.put("status",        existing_.getStatus());
                entry.put("absenceReason", existing_.getAbsenceReason());
                entry.put("recorded",      true);
            } else {
                entry.put("status",        "");
                entry.put("absenceReason", "");
                entry.put("recorded",      false);
            }

            String cadetRegion = cadet.getProject();
            grouped.computeIfAbsent(cadetRegion, k -> new ArrayList<>())
                   .add(entry);
        }

        result.put("success",  true);
        result.put("meeting",  meeting);
        result.put("register", grouped);
        result.put("totalCadets", cadets.size());
        result.put("recorded", existing.size());
        return result;
    }

    /**
     * SUBMIT REGISTER — The core method.
     *
     * Enforces:
     * 1. Every cadet must have a status (PRESENT, ABSENT, LATE)
     * 2. Every ABSENT cadet must have a reason — cannot proceed without it
     * 3. Attendance percentage is recalculated for each cadet after saving
     * 4. Auto Yellow Flag triggered if cadet has 2 consecutive absences
     *
     * Returns a detailed result including any cadets who were auto-flagged.
     */
    public Map<String, Object> submitRegister(Long meetingId,
                                               List<Map<String, Object>> entries,
                                               String recordedBy) {
        Map<String, Object> result = new HashMap<>();

        Optional<Meeting> optional = meetingRepo.findById(meetingId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "Meeting not found.");
            return result;
        }

        Meeting meeting = optional.get();

        // ── VALIDATION PASS ───────────────────────────────────
        // Check every entry before saving anything.
        // If any absent cadet is missing a reason, reject the
        // entire submission and tell the PM exactly who is missing.

        List<String> missingReasons = new ArrayList<>();

        for (Map<String, Object> entry : entries) {
            String status = (String) entry.get("status");
            String reason = (String) entry.getOrDefault("absenceReason", "");
            String name   = (String) entry.getOrDefault("fullName", "Unknown");

            if (status == null || status.isBlank()) {
                result.put("success", false);
                result.put("message",
                    "All cadets must have a status. " +
                    name + " has no status selected.");
                return result;
            }

            if ("ABSENT".equals(status) &&
                (reason == null || reason.isBlank())) {
                missingReasons.add(name);
            }
        }

        // Block submission if any absent cadet has no reason
        if (!missingReasons.isEmpty()) {
            result.put("success", false);
            result.put("message",
                "Absence reason is required for: " +
                String.join(", ", missingReasons) +
                ". Please add a reason before submitting.");
            result.put("missingReasons", missingReasons);
            return result;
        }

        // ── SAVE PASS ─────────────────────────────────────────
        // All entries are valid — save them now.

        List<String> autoFlagged = new ArrayList<>();
        List<AttendanceRecord> toSave = new ArrayList<>();

        for (Map<String, Object> entry : entries) {
            Long cadetId  = Long.valueOf(entry.get("cadetId").toString());
            String status = (String) entry.get("status");
            String reason = (String) entry.getOrDefault("absenceReason", "");
            String region = (String) entry.getOrDefault("region", "");
            String name   = (String) entry.getOrDefault("fullName", "");
            String code   = (String) entry.getOrDefault("cadetCode", "");

            // Skip if already recorded for this meeting
            if (attendanceRepo.existsByCadetIdAndMeetingId(cadetId, meetingId)) {
                continue;
            }

            AttendanceRecord record = new AttendanceRecord(
                meetingId, cadetId, name, code,
                region, status, reason,
                meeting.getMeetingDate(), recordedBy
            );
            toSave.add(record);
        }

        attendanceRepo.saveAll(toSave);

        // ── UPDATE ATTENDANCE % ───────────────────────────────
        // Recalculate each cadet's attendance percentage
        // and save it back to their profile.

        for (Map<String, Object> entry : entries) {
            Long cadetId = Long.valueOf(entry.get("cadetId").toString());

            Optional<Cadet> cadetOpt = cadetRepo.findById(cadetId);
            if (cadetOpt.isEmpty()) continue;

            Cadet cadet = cadetOpt.get();

            Double pct = attendanceRepo.calculateAttendancePercent(cadetId);
            if (pct != null) {
                cadet.setAttendancePercent((int) Math.round(pct));
                cadetRepo.save(cadet);
            }

            // ── AUTO FLAG CHECK ───────────────────────────────
            // Rule: 2 consecutive absences → Yellow Flag
            // We check the cadet's last 2 attendance records.
            // If both are ABSENT and cadet has no current flag,
            // issue a Yellow Flag automatically.

            if ("ABSENT".equals(entry.get("status")) &&
                cadet.getFlagStatus().equals("NONE")) {

                long consecutiveAbsences = countConsecutiveAbsences(cadetId);

                if (consecutiveAbsences >= 2) {
                    cadetService.issueYellowFlag(
                        cadetId,
                        "System (auto-flag)",
                        "2 consecutive absences recorded in the register."
                    );
                    autoFlagged.add(cadet.getFullName());
                }
            }
        }

        // Mark register as complete
        meeting.setRegisterComplete(true);
        meetingRepo.save(meeting);

        result.put("success", true);
        result.put("message", "Register submitted successfully.");
        result.put("saved",   toSave.size());
        result.put("autoFlagged", autoFlagged);

        if (!autoFlagged.isEmpty()) {
            result.put("flagNotice",
                "Yellow Flag automatically issued for: " +
                String.join(", ", autoFlagged) +
                " — 2 consecutive absences recorded.");
        }

        return result;
    }

    /**
     * Count how many of a cadet's last 2 attendance records
     * are ABSENT. Used to enforce the consecutive absence rule.
     */
    private long countConsecutiveAbsences(Long cadetId) {
        List<AttendanceRecord> history =
            attendanceRepo.findByCadetIdOrderByMeetingDateDesc(cadetId);

        if (history.size() < 2) return history.stream()
            .filter(r -> "ABSENT".equals(r.getStatus())).count();

        long count = 0;
        for (int i = 0; i < 2; i++) {
            if ("ABSENT".equals(history.get(i).getStatus())) count++;
        }
        return count;
    }

    // ── Attendance Reports ────────────────────────────────────

    /**
     * Get full attendance history for one cadet.
     * Shows every meeting, their status, and absence reasons.
     */
    public Map<String, Object> getCadetAttendanceReport(Long cadetId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Cadet> cadetOpt = cadetRepo.findById(cadetId);
        if (cadetOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cadet not found.");
            return result;
        }

        Cadet cadet = cadetOpt.get();
        List<AttendanceRecord> history =
            attendanceRepo.findByCadetIdOrderByMeetingDateDesc(cadetId);

        long totalMeetings = attendanceRepo.countByCadetId(cadetId);
        long attended = attendanceRepo
            .countByCadetIdAndStatus(cadetId, "PRESENT")
            + attendanceRepo.countByCadetIdAndStatus(cadetId, "LATE");
        long absences = attendanceRepo
            .countByCadetIdAndStatus(cadetId, "ABSENT");

        result.put("success",       true);
        result.put("cadet",         cadet);
        result.put("history",       history);
        result.put("totalMeetings", totalMeetings);
        result.put("attended",      attended);
        result.put("absences",      absences);
        result.put("attendancePct", cadet.getAttendancePercent());
        return result;
    }

    /**
     * Get attendance summary per region.
     * Used for the dashboard attendance by region chart.
     */
    public Map<String, Object> getRegionAttendanceSummary() {
        Map<String, Object> result = new HashMap<>();

        List<Object[]> raw = attendanceRepo.findAttendancePercentByRegion();
        Map<String, Integer> byRegion = new LinkedHashMap<>();

        for (Object[] row : raw) {
            String region = (String) row[0];
            Double pct    = (Double) row[1];
            byRegion.put(region, pct != null ? (int) Math.round(pct) : 0);
        }

        result.put("success",  true);
        result.put("byRegion", byRegion);
        return result;
    }

    /**
     * Get attendance report for a specific meeting.
     * Shows present, absent, late counts and all records.
     */
    public Map<String, Object> getMeetingAttendanceReport(Long meetingId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Meeting> optional = meetingRepo.findById(meetingId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "Meeting not found.");
            return result;
        }

        Meeting meeting = optional.get();
        List<AttendanceRecord> records =
            attendanceRepo.findByMeetingId(meetingId);

        long present = records.stream()
            .filter(r -> "PRESENT".equals(r.getStatus())).count();
        long absent  = records.stream()
            .filter(r -> "ABSENT".equals(r.getStatus())).count();
        long late    = records.stream()
            .filter(r -> "LATE".equals(r.getStatus())).count();

        // Group records by region
        Map<String, List<AttendanceRecord>> grouped = new LinkedHashMap<>();
        for (AttendanceRecord r : records) {
            grouped.computeIfAbsent(r.getRegion(), k -> new ArrayList<>())
                   .add(r);
        }

        result.put("success", true);
        result.put("meeting", meeting);
        result.put("records", records);
        result.put("grouped", grouped);
        result.put("present", present);
        result.put("absent",  absent);
        result.put("late",    late);
        result.put("total",   records.size());
        return result;
    }
}