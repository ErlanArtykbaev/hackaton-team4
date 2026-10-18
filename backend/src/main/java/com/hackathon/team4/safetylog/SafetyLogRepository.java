package com.hackathon.team4.safetylog;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SafetyLogRepository extends JpaRepository<SafetyLogEntry, UUID> {

    List<SafetyLogEntry> findByAtAfterOrderByAtDesc(Instant since);

    List<SafetyLogEntry> findByAtAfterAndRvcOrderByAtDesc(Instant since, int rvc);

    List<SafetyLogEntry> findByAtAfterAndTypeInOrderByAtDesc(Instant since, Collection<LogEntryType> types);

    List<SafetyLogEntry> findByAtAfterAndRvcAndTypeInOrderByAtDesc(Instant since, int rvc,
            Collection<LogEntryType> types);

    long countByAtAfterAndType(Instant since, LogEntryType type);

    /** {@code null} when nothing was acknowledged yet. */
    @Query("select avg(e.minutesToAck) from SafetyLogEntry e where e.at > :since and e.type = :type")
    Double averageMinutesToAck(@Param("since") Instant since, @Param("type") LogEntryType type);
}
