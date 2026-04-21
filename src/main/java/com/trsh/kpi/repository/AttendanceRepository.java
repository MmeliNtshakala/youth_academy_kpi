package com.trsh.kpi.repository;

import com.trsh.kpi.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    // Get all attendance records for one meeting
    List<AttendanceRecord> findByMeetingId(Long meetingId);

    // Get full attendance history for one cadet
    List<AttendanceRecord> findByCadetIdOrderByMeetingDateDesc(Long cadetId);

    // Get attendance records for one cadet in a date range
    List<AttendanceRecord> findByCadetIdAndMeetingDateBetween(
        Long cadetId, LocalDate from, LocalDate to
    );

    // Count how many meetings a cadet attended
    long countByCadetIdAndStatus(Long cadetId, String status);

    // Count total meetings recorded for a cadet
    long countByCadetId(Long cadetId);

    // Count absences for a cadet in the last N days
    // Used to trigger Yellow Flag after 2 consecutive absences
    @Query("SELECT COUNT(a) FROM AttendanceRecord a " +
           "WHERE a.cadetId = :cadetId " +
           "AND a.status = 'ABSENT' " +
           "AND a.meetingDate >= :since")
    long countRecentAbsences(
        @Param("cadetId") Long cadetId,
        @Param("since") LocalDate since
    );

    // Count consecutive absences — checks last 2 meetings only
    // Returns number of ABSENT records in the cadet's last 2 entries
    @Query("SELECT COUNT(a) FROM AttendanceRecord a " +
           "WHERE a.cadetId = :cadetId " +
           "AND a.status = 'ABSENT' " +
           "AND a.meetingDate IN (" +
           "  SELECT a2.meetingDate FROM AttendanceRecord a2 " +
           "  WHERE a2.cadetId = :cadetId " +
           "  ORDER BY a2.meetingDate DESC" +
           ")")
    long countLastTwoAbsences(@Param("cadetId") Long cadetId);

    // Get attendance records grouped by region for a meeting
    List<AttendanceRecord> findByMeetingIdAndRegion(
        Long meetingId, String region
    );

    // Get all records for a region in a date range — for reports
    List<AttendanceRecord> findByRegionAndMeetingDateBetween(
        String region, LocalDate from, LocalDate to
    );

    // Calculate attendance percentage for a cadet
    @Query("SELECT " +
           "COUNT(CASE WHEN a.status = 'PRESENT' THEN 1 " +
           "WHEN a.status = 'LATE' THEN 1 END) * 100.0 / COUNT(a) " +
           "FROM AttendanceRecord a " +
           "WHERE a.cadetId = :cadetId")
    Double calculateAttendancePercent(@Param("cadetId") Long cadetId);

    // Check if a register has already been taken for a cadet
    // at a specific meeting — prevents duplicate entries
    boolean existsByCadetIdAndMeetingId(Long cadetId, Long meetingId);

    // Get attendance by region for dashboard metrics
    @Query("SELECT a.region, " +
           "COUNT(CASE WHEN a.status = 'PRESENT' THEN 1 " +
           "WHEN a.status = 'LATE' THEN 1 END) * 100.0 / COUNT(a) " +
           "FROM AttendanceRecord a GROUP BY a.region")
    List<Object[]> findAttendancePercentByRegion();
}