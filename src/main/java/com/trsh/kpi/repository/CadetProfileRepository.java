package com.trsh.kpi.repository;

import com.trsh.kpi.model.CadetProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CadetProfileRepository
        extends JpaRepository<CadetProfile, Long> {

    // Get full profile history for one cadet newest first
    List<CadetProfile> findByCadetIdOrderByRecordedDateDesc(
        Long cadetId
    );

    // Get the most recent profile entry for one cadet
    Optional<CadetProfile> findFirstByCadetIdOrderByRecordedDateDesc(
        Long cadetId
    );

    // Get all profiles recorded by a specific Liaison
    List<CadetProfile> findByRecordedBy(String recordedBy);

    // Get all profiles in a region
    List<CadetProfile> findByRegion(String region);

    // Get all profiles with a specific wellbeing status
    // Used for dashboard — how many cadets are RED/AMBER
    List<CadetProfile> findByWellbeing(String wellbeing);

    // Get all profiles with pressing info
    // Used to alert Liaisons of urgent matters
    List<CadetProfile> findByPressingInfoIsNotNull();

    // Count cadets by wellbeing in a region
    long countByRegionAndWellbeing(String region, String wellbeing);
}