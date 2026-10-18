package com.hackathon.team4.kitchen;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, UUID> {

    /** The outlet's tickets since a moment (the current shift), newest first. */
    @EntityGraph(attributePaths = "lines")
    List<KitchenTicket> findByRvcAndSentAtAfterOrderBySentAtDesc(int rvc, Instant since);

    /** A round is turned into a ticket once, even if its event comes twice. */
    boolean existsByRvcAndCheckSeqAndRound(int rvc, String checkSeq, int round);

    Optional<KitchenTicket> findByRvcAndCheckSeqAndRound(int rvc, String checkSeq, int round);
}
