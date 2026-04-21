package com.trsh.kpi.repository;

import com.trsh.kpi.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // Get all meetings for a specific region
    List<Meeting> findByRegionOrderByMeetingDateDesc(String region);

    // Get all meetings on a specific date
    List<Meeting> findByMeetingDate(LocalDate date);

    // Get all meetings of a type e.g. "Sunday Session"
    List<Meeting> findByMeetingType(String meetingType);

    // Get all incomplete registers — PM hasn't finished yet
    List<Meeting> findByRegisterCompleteFalse();

    // Get all completed registers
    List<Meeting> findByRegisterCompleteTrue();

    // Get all meetings between two dates — used for reports
    List<Meeting> findByMeetingDateBetweenOrderByMeetingDateDesc(
        LocalDate from, LocalDate to
    );

    // Get all meetings for a region between two dates
    List<Meeting> findByRegionAndMeetingDateBetween(
        String region, LocalDate from, LocalDate to
    );

    // Count total meetings per region
    long countByRegion(String region);

    // Get most recent meetings — for dashboard
    List<Meeting> findTop5ByOrderByMeetingDateDesc();
}