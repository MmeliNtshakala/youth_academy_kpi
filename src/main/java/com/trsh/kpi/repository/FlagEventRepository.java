package com.trsh.kpi.repository;

import com.trsh.kpi.model.FlagEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FlagEventRepository extends JpaRepository<FlagEvent, Long> {

// Get full flag history for one cadet
    List<FlagEvent> findByCadetIdOrderByEventDateDesc(Long cadetId);

// Get all events of a specific type e.g. all YELLOW events
    List<FlagEvent> findByEventType(String eventType);

// Get recent activity across all cadets — for dashboard timeline
    List<FlagEvent> findTop10ByOrderByEventDateDesc();

// Count how many Orange flags a cadet received within a date range
// Used to enforce the "2 Orange flags in 12 months = RED" rule
    long countByCadetIdAndEventTypeAndEventDateBetween(
        Long cadetId,
        String eventType,
        LocalDate from,
        LocalDate to
    );

// Get all events triggered by a specific PM
    List<FlagEvent> findByTriggeredBy(String triggeredBy);
}