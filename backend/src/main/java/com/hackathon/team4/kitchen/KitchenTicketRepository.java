package com.hackathon.team4.kitchen;

import com.hackathon.team4.allergen.Verdict;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, UUID> {

    /** The outlet's tickets since a moment (the current shift), newest first. */
    @EntityGraph(attributePaths = "lines")
    List<KitchenTicket> findByRvcAndSentAtAfterOrderBySentAtDesc(int rvc, Instant since);

    /** A round is turned into a ticket once, even if its event comes twice. */
    boolean existsByRvcAndCheckSeqAndRound(int rvc, String checkSeq, int round);

    @Query("""
            select count(l) from KitchenTicket t join t.lines l
            where t.sentAt > :since and t.source = :source and l.verdict in :verdicts
            """)
    long countLines(@Param("since") Instant since, @Param("source") TicketSource source,
            @Param("verdicts") Collection<Verdict> verdicts);
}
