package com.hackathon.team4.allergy;

import com.hackathon.team4.kitchen.AckAction;
import com.hackathon.team4.kitchen.KitchenTicket;
import com.hackathon.team4.kitchen.KitchenTicketRepository;
import com.hackathon.team4.kitchen.TicketLine;
import com.hackathon.team4.kitchen.TicketSource;
import com.hackathon.team4.kitchen.TicketStatus;
import com.hackathon.team4.passport.MemberAllergy;
import com.hackathon.team4.passport.Passport;
import com.hackathon.team4.passport.PassportMember;
import com.hackathon.team4.passport.PassportRepository;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogEntry;
import com.hackathon.team4.safetylog.SafetyLogRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Liquibase schema matches the entities (Hibernate validates it on start) and every value survives a round trip.
 */
@DataJpaTest
@ActiveProfiles("test")
class AllergyStorageTest {

    private static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PassportRepository passports;

    @Autowired
    private KitchenTicketRepository tickets;

    @Autowired
    private SafetyLogRepository log;

    @Test
    void shouldStorePassportMembersWithAllergiesInOrder() {
        Passport passport = new Passport("R-203844", NOW);
        passport.describe("518", "Kuznetsov Mikhail", null, "front-desk", NOW);
        passport.addMember(NOW).update("Mikhail", null, true, Set.of(new MemberAllergy(Allergen.PEANUTS,
                Severity.INTOLERANCE)), NOW);
        passport.addMember(NOW).update("Misha", 7, false, Set.of(new MemberAllergy(Allergen.PEANUTS,
                Severity.SEVERE)), NOW);
        passport.addMember(NOW).update("Anna", null, false, Set.of(new MemberAllergy(Allergen.MILK,
                Severity.INTOLERANCE), new MemberAllergy(Allergen.EGGS, Severity.INTOLERANCE)), NOW);
        passports.save(passport);
        em.flush();
        em.clear();

        Passport stored = passports.findByReservationId("R-203844").orElseThrow();

        assertThat(stored.members()).extracting(PassportMember::name).containsExactly("Mikhail", "Misha", "Anna");
        PassportMember misha = stored.members().get(1);
        assertThat(misha.age()).isEqualTo(7);
        assertThat(misha.allergies()).singleElement().satisfies(allergy -> {
            assertThat(allergy.allergen()).isEqualTo(Allergen.PEANUTS);
            assertThat(allergy.severity()).isEqualTo(Severity.SEVERE);
        });
        assertThat(stored.members().get(2).allergies()).extracting(MemberAllergy::allergen)
                .containsExactlyInAnyOrder(Allergen.MILK, Allergen.EGGS);
        assertThat(passports.findByReservationIdIn(List.of("R-203844", "R-000000"))).hasSize(1);
    }

    @Test
    void shouldRemoveDroppedMembersAndKeepTheNewOrder() {
        Passport passport = new Passport("R-203478", NOW);
        PassportMember first = passport.addMember(NOW);
        first.update("First", null, true, Set.of(), NOW);
        PassportMember second = passport.addMember(NOW);
        second.update("Second", 5, false, Set.of(new MemberAllergy(Allergen.FISH, Severity.SEVERE)), NOW);
        PassportMember third = passport.addMember(NOW);
        third.update("Third", null, false, Set.of(), NOW);
        passports.save(passport);
        em.flush();
        em.clear();

        Passport stored = passports.findByReservationId("R-203478").orElseThrow();
        stored.keepMembers(List.of(stored.member(third.id()).orElseThrow(), stored.member(first.id()).orElseThrow()));
        em.flush();
        em.clear();

        assertThat(passports.findByReservationId("R-203478").orElseThrow().members())
                .extracting(PassportMember::name).containsExactly("Third", "First");
    }

    @Test
    void shouldStoreTicketLinesWithVerdictsAndAcknowledgement() {
        DangerFor misha = new DangerFor(UUID.randomUUID(), "Misha", Allergen.PEANUTS, Severity.SEVERE);
        KitchenTicket ticket = new KitchenTicket(4, "1101", "83400", 1, "P3", "R-203844", "518", "Kuznetsov",
                NOW, TicketSource.CHECKED, List.of(
                        new TicketLine("Chicken satay", 1, List.of("Peanut sauce"), List.of(Allergen.PEANUTS),
                                Verdict.DANGER, List.of(misha), "Mikhail", "adult, served separately", "W02"),
                        new TicketLine("Kids nuggets", 1, List.of(), List.of(Allergen.GLUTEN), Verdict.SAFE,
                                List.of(), "Misha", null, "W02")),
                true, NOW);
        tickets.save(ticket);
        em.flush();
        em.clear();

        KitchenTicket stored = tickets.findWithLinesById(ticket.id()).orElseThrow();
        stored.acknowledge(AckAction.PREPARED_SEPARATELY, "Cook 1", NOW.plusSeconds(60));
        em.flush();
        em.clear();

        KitchenTicket acknowledged = tickets.findWithLinesById(ticket.id()).orElseThrow();
        assertThat(acknowledged.status()).isEqualTo(TicketStatus.ACKNOWLEDGED);
        assertThat(acknowledged.ackAction()).contains(AckAction.PREPARED_SEPARATELY);
        assertThat(acknowledged.lines()).extracting(TicketLine::name).containsExactly("Chicken satay", "Kids nuggets");
        TicketLine satay = acknowledged.lines().get(0);
        assertThat(satay.condiments()).containsExactly("Peanut sauce");
        assertThat(satay.allergens()).containsExactly(Allergen.PEANUTS);
        assertThat(satay.dangerFor()).containsExactly(misha);
        assertThat(satay.overrideReason()).isEqualTo("adult, served separately");
        assertThat(tickets.existsByRvcAndCheckSeqAndRound(4, "83400", 1)).isTrue();
        assertThat(tickets.existsByRvcAndCheckSeqAndRound(4, "83400", 2)).isFalse();
    }

    @Test
    void shouldFindOnlyUnacknowledgedDangerousTicketsOlderThanTheLimit() {
        KitchenTicket late = ticket("1", true, NOW.minus(Duration.ofMinutes(4)));
        KitchenTicket fresh = ticket("2", true, NOW.minus(Duration.ofMinutes(1)));
        KitchenTicket safe = ticket("3", false, NOW.minus(Duration.ofMinutes(10)));
        KitchenTicket done = ticket("4", true, NOW.minus(Duration.ofMinutes(10)));
        done.acknowledge(AckAction.DISH_CHANGED, "Cook 1", NOW);
        tickets.saveAll(List.of(late, fresh, safe, done));
        em.flush();
        em.clear();

        List<KitchenTicket> overdue = tickets.findByDangerousTrueAndStatusAndSentAtBefore(TicketStatus.NEW,
                NOW.minus(Duration.ofMinutes(3)));

        assertThat(overdue).extracting(KitchenTicket::checkSeq).containsExactly("1");
        assertThat(tickets.countByDangerousTrueAndStatusNot(TicketStatus.ACKNOWLEDGED)).isEqualTo(2);
    }

    @Test
    void shouldFilterLogNewestFirstAndAverageTimeToAcknowledge() {
        Instant shift = NOW.minus(Duration.ofHours(8));
        log.saveAll(List.of(
                SafetyLogEntry.of(LogEntryType.PASSPORT_UPDATED, NOW.minusSeconds(300))
                        .reservation("R-203844", "518").actor("front-desk").build(),
                SafetyLogEntry.of(LogEntryType.OVERRIDE, NOW.minusSeconds(200)).outlet(4, "Pool Bar")
                        .member("Mikhail").item("Chicken satay", Allergen.PEANUTS).actor("W02")
                        .reason("adult, served separately").build(),
                SafetyLogEntry.of(LogEntryType.TICKET_ACKNOWLEDGED, NOW.minusSeconds(100)).outlet(4, "Pool Bar")
                        .minutesToAck(new BigDecimal("1.5")).build(),
                SafetyLogEntry.of(LogEntryType.TICKET_ACKNOWLEDGED, NOW).outlet(1, "Main Restaurant")
                        .minutesToAck(new BigDecimal("2.5")).build()));
        em.flush();
        em.clear();

        assertThat(log.findByAtAfterOrderByAtDesc(shift)).extracting(SafetyLogEntry::type).containsExactly(
                LogEntryType.TICKET_ACKNOWLEDGED, LogEntryType.TICKET_ACKNOWLEDGED, LogEntryType.OVERRIDE,
                LogEntryType.PASSPORT_UPDATED);
        assertThat(log.findByAtAfterAndRvcAndTypeInOrderByAtDesc(shift, 4, List.of(LogEntryType.OVERRIDE)))
                .singleElement().extracting(SafetyLogEntry::reason).isEqualTo("adult, served separately");
        assertThat(log.countByAtAfterAndType(shift, LogEntryType.TICKET_ACKNOWLEDGED)).isEqualTo(2);
        assertThat(log.averageMinutesToAck(shift, LogEntryType.TICKET_ACKNOWLEDGED)).isEqualTo(2.0);
        assertThat(log.averageMinutesToAck(NOW, LogEntryType.TICKET_ACKNOWLEDGED)).isNull();
    }

    private static KitchenTicket ticket(String checkSeq, boolean dangerous, Instant sentAt) {
        return new KitchenTicket(4, checkSeq, checkSeq, 1, null, null, null, null, sentAt, TicketSource.UNLINKED,
                List.of(), dangerous, sentAt);
    }
}
