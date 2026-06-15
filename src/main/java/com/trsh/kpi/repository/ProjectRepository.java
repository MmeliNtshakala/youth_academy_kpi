package com.trsh.kpi.repository;

import com.trsh.kpi.model.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository
        extends JpaRepository<ProjectEntity, Long> {

    // Get all national projects — visible to everyone
    List<ProjectEntity> findByScope(String scope);

    // Get all projects for a specific region
    // includes both provincial projects for that region
    List<ProjectEntity> findByRegion(String region);

    // Get all active projects
    List<ProjectEntity> findByStatus(String status);

    // Get all projects a specific user created
    List<ProjectEntity> findByCreatedBy(String createdBy);

    // Get all national + provincial projects for a region
    // Used to show a Liaison all relevant projects
    List<ProjectEntity> findByScopeOrRegion(
        String scope, String region
    );

    // Get all completed projects where badge not yet awarded
    List<ProjectEntity> findByStatusAndBadgeAwarded(
        String status, boolean badgeAwarded
    );

    // Count active projects per region
    long countByRegionAndStatus(String region, String status);
}