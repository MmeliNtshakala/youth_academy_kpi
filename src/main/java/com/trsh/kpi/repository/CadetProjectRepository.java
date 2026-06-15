package com.trsh.kpi.repository;

import com.trsh.kpi.model.CadetProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CadetProjectRepository
        extends JpaRepository<CadetProject, Long> {

    // Get all projects a cadet is assigned to
    List<CadetProject> findByCadetId(Long cadetId);

    // Get all cadets assigned to a project
    List<CadetProject> findByProjectId(Long projectId);

    // Get all cadets with badges earned on a project
    List<CadetProject> findByProjectIdAndBadgeEarned(
        Long projectId, boolean badgeEarned
    );

    // Get all badges earned by a cadet
    // Used to display badge wall on cadet profile
    List<CadetProject> findByCadetIdAndBadgeEarned(
        Long cadetId, boolean badgeEarned
    );

    // Check if a cadet is already assigned to a project
    boolean existsByCadetIdAndProjectId(
        Long cadetId, Long projectId
    );

    // Get a specific cadet-project link
    Optional<CadetProject> findByCadetIdAndProjectId(
        Long cadetId, Long projectId
    );

    // Get all active project assignments in a region
    List<CadetProject> findByRegionAndParticipationStatus(
        String region, String status
    );

    // Count how many cadets are on a project
    long countByProjectId(Long projectId);

    // Count how many badges earned on a project
    long countByProjectIdAndBadgeEarned(
        Long projectId, boolean badgeEarned
    );

    // Get all cadet-project records where badge not yet
    // awarded for a completed project
    // Used by the badge awarding service
    @Query("SELECT cp FROM CadetProject cp " +
           "WHERE cp.projectId = :projectId " +
           "AND cp.badgeEarned = false " +
           "AND cp.participationStatus = 'ACTIVE'")
    List<CadetProject> findUnbadgedCadets(
        @Param("projectId") Long projectId
    );
}