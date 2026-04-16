package com.trsh.kpi.reposatory;

import com.trsh.kpi.model.Cadet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CadetRepository extends JpaRepository<Cadet, Long> {

    // Find a cadet by their readable code e.g. CDT-0001
    Optional<Cadet> findByCadetCode(String cadetCode);

    // Find all cadets on a specific project
    List<Cadet> findByProject(String project);

    // Find all cadets at a specific flag stage
    List<Cadet> findByFlagStatus(String flagStatus);

    // Find all cadets assigned to a specific project manager
    List<Cadet> findByProjectManager(String projectManager);

    // Find cadets currently under correction
    List<Cadet> findByUnderCorrectionTrue();

    // Search by name (case-insensitive, partial match)
    List<Cadet> findByFullNameContainingIgnoreCase(String name);

    // Count cadets per flag status — used for dashboard metrics
    long countByFlagStatus(String flagStatus);

    // Get average attendance across all cadets
    @Query("SELECT AVG(c.attendancePercent) FROM Cadet c")
    Double findAverageAttendance();

    // Get average attendance per project
    @Query("SELECT c.project, AVG(c.attendancePercent) FROM Cadet c GROUP BY c.project")
    List<Object[]> findAverageAttendanceByProject();
}