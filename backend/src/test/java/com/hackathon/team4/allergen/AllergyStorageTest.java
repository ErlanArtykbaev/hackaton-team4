package com.hackathon.team4.allergen;

import com.hackathon.team4.kitchen.KitchenTicket;
import com.hackathon.team4.kitchen.KitchenTicketRepository;
import com.hackathon.team4.kitchen.TicketLine;
import com.hackathon.team4.kitchen.TicketSource;
import com.hackathon.team4.passport.MemberAllergy;
import com.hackathon.team4.passport.Passport;
import com.hackathon.team4.passport.PassportMember;
import com.hackathon.team4.passport.PassportRepository;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogEntry;
import com.hackathon.team4.safetylog.SafetyLogRepository;
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
        passport.describe("518", "G-203844", "Kuznetsov Mikhail", null, "front-desk", NOW);
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
    void shouldStoreTicketLinesWithVerdictsConfirmationAndConsent() {
        DangerFor misha = new DangerFor(UUID.randomUUID(), "Misha", Allergen.PEANUTS, Severity.SEVERE);
        DangerFor anna = new DangerFor(UUID.randomUUID(), "Anna", Allergen.MILK, Severity.INTOLERANCE);
        KitchenTicket ticket = new KitchenTicket(4, "1101", "83400", 1, "P3", "R-203844", "518", "Kuznetsov",
                NOW, TicketSource.CHECKED, List.of(
                        new TicketLine("Chicken satay", 1, List.of("Peanut sauce"), List.of(Allergen.PEANUTS),
                                Verdict.DANGER, List.of(misha), "Mikhail", true, null),
                        new TicketLine("Thai iced tea", 1, List.of(), List.of(Allergen.MILK), Verdict.WARNING,
                                List.of(anna), "Anna", false, "Anna Kuznetsova"),
                        new TicketLine("Kids nuggets", 1, List.of(), List.of(Allergen.GLUTEN), Verdict.SAFE,
                                List.of(), "Misha", false, null)),
                true, NOW);
        tickets.save(ticket);
        em.flush();
        em.clear();

        KitchenTicket stored = tickets.findByRvcAndSentAtAfterOrderBySentAtDesc(4, NOW.minusSeconds(1)).get(0);

        assertThat(stored.hasAllergens()).isTrue();
        assertThat(stored.lines()).extracting(TicketLine::name)
                .containsExactly("Chicken satay", "Thai iced tea", "Kids nuggets");
        TicketLine satay = stored.lines().get(0);
        assertThat(satay.condiments()).containsExactly("Peanut sauce");
        assertThat(satay.allergens()).containsExactly(Allergen.PEANUTS);
        assertThat(satay.dangerFor()).containsExactly(misha);
        assertThat(satay.waiterConfirmed()).isTrue();
        assertThat(stored.lines().get(1).consentBy()).isEqualTo("Anna Kuznetsova");
        assertThat(tickets.existsByRvcAndCheckSeqAndRound(4, "83400", 1)).isTrue();
        assertThat(tickets.existsByRvcAndCheckSeqAndRound(4, "83400", 2)).isFalse();
    }

    @Test
    void shouldListOnlyTheOutletsTicketsOfTheShiftNewestFirst() {
        tickets.saveAll(List.of(
                ticket("1", 4, NOW.minus(Duration.ofHours(10))),
                ticket("2", 4, NOW.minus(Duration.ofMinutes(5))),
                ticket("3", 4, NOW.minus(Duration.ofMinutes(1))),
                ticket("4", 1, NOW.minus(Duration.ofMinutes(1)))));
        em.flush();
        em.clear();

        assertThat(tickets.findByRvcAndSentAtAfterOrderBySentAtDesc(4, NOW.minus(Duration.ofHours(8))))
                .extracting(KitchenTicket::checkSeq).containsExactly("3", "2");
    }

    @Test
    void shouldSearchLogByRoomOrNameNewestFirst() {
        log.saveAll(List.of(
                SafetyLogEntry.of(LogEntryType.PASSPORT_UPDATED, NOW.minusSeconds(300))
                        .reservation("R-203844", "518", "Mikhail Kuznetsov").actor("front-desk").build(),
                SafetyLogEntry.of(LogEntryType.GUEST_CONSENT, NOW.minusSeconds(200)).outlet(4, "Pool Bar")
                        .reservation("R-203844", "518", "Mikhail Kuznetsov").member("Anna")
                        .item("Thai iced tea").allergen(Allergen.MILK, Severity.INTOLERANCE).actor("W02")
                        .statement("I know it contains MILK and I order it").build(),
                SafetyLogEntry.of(LogEntryType.ORDER_PLACED, NOW).outlet(1, "Main Restaurant")
                        .reservation("R-203478", "223", "Jane Doe").actor("W01").build()));
        em.flush();
        em.clear();

        assertThat(log.search(null, null, null, null)).extracting(SafetyLogEntry::type).containsExactly(
                LogEntryType.ORDER_PLACED, LogEntryType.GUEST_CONSENT, LogEntryType.PASSPORT_UPDATED);
        assertThat(log.search(null, null, "518", null)).hasSize(2);
        assertThat(log.search(null, null, null, "kuzn")).hasSize(2);
        assertThat(log.search(4, LogEntryType.GUEST_CONSENT, null, null)).singleElement().satisfies(entry -> {
            assertThat(entry.severity()).isEqualTo(Severity.INTOLERANCE);
            assertThat(entry.statement()).isEqualTo("I know it contains MILK and I order it");
        });
        assertThat(log.countByAtAfterAndType(NOW.minusSeconds(250), LogEntryType.GUEST_CONSENT)).isEqualTo(1);
    }

    private static KitchenTicket ticket(String checkSeq, int rvc, Instant sentAt) {
        return new KitchenTicket(rvc, checkSeq, checkSeq, 1, null, null, null, null, sentAt, TicketSource.UNLINKED,
                List.of(), false, sentAt);
    }
}
