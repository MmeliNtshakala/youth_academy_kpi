package com.trsh.kpi.repository;

import com.trsh.kpi.model.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneRepository
        extends JpaRepository<Milestone, Long> {

    // Get all milestones for a project ordered by position
    List<Milestone> findByProjectIdOrderByMilestoneOrderAsc(
        Long projectId
    );

    // Get all completed milestones for a project
    List<Milestone> findByProjectIdAndCompleted(
        Long projectId, boolean completed
    );

    // Count completed milestones for a project
    long countByProjectIdAndCompleted(
        Long projectId, boolean completed
    );

    // Count total milestones for a project
    long countByProjectId(Long projectId);

    // Get all overdue milestones across all projects
    // Used for dashboard alerts
    List<Milestone> findByCompletedFalseAndTargetDateBefore(
        java.time.LocalDate date
    );
}