package com.hackathon.team4.safetylog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SafetyLogRepository extends JpaRepository<SafetyLogEntry, UUID> {

    /**
     * Newest first; every filter is optional ({@code null}). {@code name} is a lower-case fragment matched against the
     * reservation's guest and the member.
     */
    @Query("""
            select e from SafetyLogEntry e
            where (:rvc is null or e.rvc = :rvc)
              and (:type is null or e.type = :type)
              and (:room is null or e.roomNumber = :room)
              and (:name is null or lower(e.guestName) like concat('%', :name, '%')
                                 or lower(e.memberName) like concat('%', :name, '%'))
            order by e.at desc
            """)
    List<SafetyLogEntry> search(@Param("rvc") Integer rvc, @Param("type") LogEntryType type,
            @Param("room") String room, @Param("name") String name);

    long countByAtAfterAndType(Instant since, LogEntryType type);
}
