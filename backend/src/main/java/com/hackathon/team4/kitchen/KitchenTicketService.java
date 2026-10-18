package com.hackathon.team4.kitchen;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.AllergenEngine;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.ItemCheck;
import com.hackathon.team4.allergen.PartyMember;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.passport.Party;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogEntry;
import com.hackathon.team4.safetylog.SafetyLogService;
import com.hackathon.team4.sandbox.SandboxException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kitchen tickets: every round sent to the kitchen with its allergens and for whom they are dangerous. The kitchen is
 * notified only; it never approves.
 */
@Service
public class KitchenTicketService {

    private static final Logger log = LoggerFactory.getLogger(KitchenTicketService.class);

    /** The display shows one shift. */
    static final Duration SHIFT = Duration.ofHours(12);

    /** One line of a round. {@code allergens} already include the chosen condiments. */
    public record RoundLine(String name, int quantity, List<String> condiments, Set<Allergen> allergens,
            UUID forMemberId, boolean waiterConfirmed, String consentBy) {

        public RoundLine {
            condiments = condiments == null ? List.of() : List.copyOf(condiments);
            allergens = allergens == null ? Set.of() : Set.copyOf(allergens);
        }

        /** A line typed straight into the POS: no "for whom", no confirmation, no consent. */
        public static RoundLine fromPos(String name, int quantity, List<String> condiments, Set<Allergen> allergens) {
            return new RoundLine(name, quantity, condiments, allergens, null, false, null);
        }
    }

    /** A round as the POS reports it; {@code reservationId} is {@code null} for a check without a guest. */
    public record Round(int rvc, String outletName, String checkNumber, String checkSeq, int round, String table,
            String reservationId, Instant sentAt, List<RoundLine> lines) {

        public Round {
            lines = List.copyOf(lines);
        }
    }

    private final KitchenTicketRepository tickets;
    private final PassportService passports;
    private final AllergenEngine engine;
    private final SafetyLogService safetyLog;
    private final Clock clock;

    KitchenTicketService(KitchenTicketRepository tickets, PassportService passports, AllergenEngine engine,
            SafetyLogService safetyLog, Clock clock) {
        this.tickets = tickets;
        this.passports = passports;
        this.engine = engine;
        this.safetyLog = safetyLog;
        this.clock = clock;
    }

    /** The outlet's tickets of the current shift, newest first. */
    @Transactional(readOnly = true)
    public List<KitchenTicket> tickets(int rvc) {
        return tickets.findByRvcAndSentAtAfterOrderBySentAtDesc(rvc, clock.instant().minus(SHIFT));
    }

    /**
     * A round ordered through our tablet, called right after the POS accepted it. Replaces the ticket the event feed
     * may already have made for the same round, since this one knows for whom each line is and who consented.
     */
    @Transactional
    public KitchenTicket recordCheckedRound(Round round) {
        tickets.findByRvcAndCheckSeqAndRound(round.rvc(), round.checkSeq(), round.round()).ifPresent(tickets::delete);
        tickets.flush();
        Party party = passports.party(round.reservationId());
        KitchenTicket ticket = tickets.save(ticket(round, TicketSource.CHECKED, party));
        log.info("Kitchen ticket for check {} round {} at rvc {} (checked)", round.checkNumber(), round.round(),
                round.rvc());
        return ticket;
    }

    /**
     * A {@code pos.check.round_sent} event. Rounds already recorded (ours, or a duplicate event) are skipped. A round
     * for a reservation is "not checked at ordering" and logged when it contains an allergen of someone at the table;
     * a round without a reservation shows allergens only.
     */
    @Transactional
    public Optional<KitchenTicket> recordPosRound(Round round) {
        if (tickets.existsByRvcAndCheckSeqAndRound(round.rvc(), round.checkSeq(), round.round())) {
            return Optional.empty();
        }
        if (round.reservationId() == null) {
            return Optional.of(tickets.save(ticket(round, TicketSource.UNLINKED, null)));
        }
        Party party = partyOrNull(round.reservationId());
        KitchenTicket ticket = tickets.save(ticket(round, TicketSource.UNCHECKED, party));
        if (ticket.hasAllergens()) {
            safetyLog.append(uncheckedEntry(round, ticket));
        }
        log.info("Kitchen ticket for check {} round {} at rvc {} (not checked at ordering)", round.checkNumber(),
                round.round(), round.rvc());
        return Optional.of(ticket);
    }

    private KitchenTicket ticket(Round round, TicketSource source, Party party) {
        List<PartyMember> members = party == null ? List.of() : party.members();
        List<TicketLine> lines = round.lines().stream().map(line -> line(line, members)).toList();
        boolean hasAllergens = party == null
                ? lines.stream().anyMatch(line -> !line.allergens().isEmpty())
                : lines.stream().anyMatch(line -> line.verdict() == Verdict.WARNING || line.verdict() == Verdict.DANGER);
        return new KitchenTicket(round.rvc(), round.checkNumber(), round.checkSeq(), round.round(), round.table(),
                round.reservationId(), party == null ? null : party.roomNumber(),
                party == null ? null : party.guestName(), round.sentAt(), source, lines, hasAllergens, clock.instant());
    }

    private TicketLine line(RoundLine line, List<PartyMember> members) {
        ItemCheck check = engine.checkItem(members, line.allergens());
        String forMemberName = members.stream()
                .filter(member -> member.id().equals(line.forMemberId()))
                .map(PartyMember::name)
                .findFirst()
                .orElse(null);
        return new TicketLine(line.name(), line.quantity(), line.condiments(), line.allergens().stream().sorted()
                .toList(), check.verdict(), check.dangerFor(), forMemberName, line.waiterConfirmed(),
                line.consentBy());
    }

    /** Without the party (the PMS failed) the kitchen still sees the allergens: better than no ticket. */
    private Party partyOrNull(String reservationId) {
        try {
            return passports.party(reservationId);
        } catch (SandboxException failure) {
            log.warn("No party for reservation {} ({}); ticket shows allergens only", reservationId, failure.code());
            return null;
        }
    }

    private SafetyLogEntry uncheckedEntry(Round round, KitchenTicket ticket) {
        List<TicketLine> dangerous = ticket.lines().stream().filter(line -> !line.dangerFor().isEmpty()).toList();
        List<DangerFor> dangers = dangerous.stream().flatMap(line -> line.dangerFor().stream()).toList();
        SafetyLogEntry.Builder entry = safetyLog.entry(LogEntryType.POS_ORDER_UNCHECKED)
                .outlet(round.rvc(), round.outletName())
                .reservation(round.reservationId(), ticket.roomNumber(), ticket.guestName())
                .item(dangerous.stream().map(TicketLine::name).distinct().collect(Collectors.joining(", ")))
                .actor("POS")
                .statement("Not checked at ordering: " + dangerous.stream()
                        .map(line -> line.name() + " — " + line.dangerFor().stream()
                                .map(danger -> danger.allergen() + " for " + danger.memberName() + " ("
                                        + danger.severity().name().toLowerCase() + ")")
                                .collect(Collectors.joining(", ")))
                        .collect(Collectors.joining("; ")));
        if (dangers.size() == 1) {
            DangerFor only = dangers.get(0);
            entry.member(only.memberName()).allergen(only.allergen(), only.severity());
        } else {
            entry.member(dangers.stream().map(DangerFor::memberName).filter(Objects::nonNull).distinct()
                    .collect(Collectors.joining(", ")));
        }
        return entry.build();
    }
}
