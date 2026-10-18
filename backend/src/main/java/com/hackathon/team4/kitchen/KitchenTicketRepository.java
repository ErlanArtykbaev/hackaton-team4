package com.hackathon.team4.kitchen;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, UUID> {

    @EntityGraph(attributePaths = "lines")
    Optional<KitchenTicket> findWithLinesById(UUID id);

    /** The outlet's tickets since a moment (the current shift); the service orders them for the display. */
    @EntityGraph(attributePaths = "lines")
    List<KitchenTicket> findByRvcAndSentAtAfterOrderBySentAtDesc(int rvc, Instant since);

    /** Dangerous tickets nobody acknowledged in time: candidates for escalation. */
    @EntityGraph(attributePaths = "lines")
    List<KitchenTicket> findByDangerousTrueAndStatusAndSentAtBefore(TicketStatus status, Instant sentBefore);

    /** A round is turned into a ticket once, even if its event comes twice. */
    boolean existsByRvcAndCheckSeqAndRound(int rvc, String checkSeq, int round);

    long countByDangerousTrueAndStatusNot(TicketStatus status);
}
