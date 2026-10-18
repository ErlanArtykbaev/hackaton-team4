package com.hackathon.team4.kitchen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.AllergenEngine;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.kitchen.KitchenTicketService.Round;
import com.hackathon.team4.kitchen.KitchenTicketService.RoundLine;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.passport.PassportSnapshot;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogRepository;
import com.hackathon.team4.safetylog.SafetyLogService;
import com.hackathon.team4.sandbox.SandboxErrorCode;
import com.hackathon.team4.sandbox.SandboxException;
import com.hackathon.team4.sandbox.generated.model.Guest;
import com.hackathon.team4.sandbox.generated.model.Reservation;
import com.hackathon.team4.sandbox.pms.PmsGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Demo step 4: the Kuznetsov family at the Pool Bar. */
@DataJpaTest
@ActiveProfiles("test")
@Import({KitchenTicketService.class, PassportService.class, SafetyLogService.class, AllergenEngine.class,
        KitchenTicketServiceTest.FixedClock.class})
class KitchenTicketServiceTest {

    static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");
    private static final String KUZNETSOV = "R-203804";

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @MockitoBean
    private PmsGateway pms;

    @Autowired
    private KitchenTicketService kitchen;

    @Autowired
    private PassportService passports;

    @Autowired
    private SafetyLogRepository log;

    private UUID mikhail;
    private UUID anna;

    @BeforeEach
    void kuznetsovFamilyWithPassport() {
        when(pms.reservation(KUZNETSOV)).thenReturn(new Reservation().id(KUZNETSOV).roomNumber("539")
                .guest(new Guest().id("G-1").firstName("Mikhail").lastName("Kuznetsov").allergies(List.of(
                        com.hackathon.team4.sandbox.generated.model.Allergen.PEANUTS,
                        com.hackathon.team4.sandbox.generated.model.Allergen.MILK))));
        List<PassportSnapshot.Member> members = passports.save(KUZNETSOV, null, List.of(
                new PassportSnapshot.Member(null, "Mikhail", null, true, Map.of()),
                new PassportSnapshot.Member(null, "Anna", null, false, Map.of(Allergen.MILK, Severity.INTOLERANCE)),
                new PassportSnapshot.Member(null, "Misha", 7, false, Map.of(Allergen.PEANUTS, Severity.SEVERE))),
                "front-desk", PassportService.Source.FRONT_DESK, null, null).members();
        mikhail = members.get(0).id();
        anna = members.get(1).id();
    }

    @Test
    void shouldShowForWhomConfirmationAndConsentOnTabletRound() {
        KitchenTicket ticket = kitchen.recordCheckedRound(round("1", KUZNETSOV,
                new RoundLine("Thai iced tea", 1, List.of(), Set.of(Allergen.MILK), anna, false, "Anna Kuznetsova"),
                new RoundLine("Chicken satay", 1, List.of(), Set.of(Allergen.PEANUTS), mikhail, true, null)));

        assertThat(ticket.source()).isEqualTo(TicketSource.CHECKED);
        assertThat(ticket.roomNumber()).isEqualTo("539");
        assertThat(ticket.hasAllergens()).isTrue();
        TicketLine tea = ticket.lines().get(0);
        assertThat(tea.verdict()).isEqualTo(Verdict.WARNING);
        assertThat(tea.forMemberName()).isEqualTo("Anna");
        assertThat(tea.consentBy()).isEqualTo("Anna Kuznetsova");
        TicketLine satay = ticket.lines().get(1);
        assertThat(satay.verdict()).isEqualTo(Verdict.DANGER);
        assertThat(satay.forMemberName()).isEqualTo("Mikhail");
        assertThat(satay.waiterConfirmed()).isTrue();
        assertThat(satay.dangerFor()).extracting(DangerFor::memberName).containsExactly("Misha");
        assertThat(log.search(null, LogEntryType.POS_ORDER_UNCHECKED, null, null)).isEmpty();
    }

    @Test
    void shouldMarkPosRoundNotCheckedAndLogIt() {
        KitchenTicket ticket = kitchen.recordPosRound(round("2", KUZNETSOV,
                RoundLine.fromPos("Som tam", 1, List.of(), Set.of(Allergen.PEANUTS, Allergen.FISH)))).orElseThrow();

        assertThat(ticket.source()).isEqualTo(TicketSource.UNCHECKED);
        assertThat(ticket.lines().get(0).verdict()).isEqualTo(Verdict.DANGER);
        assertThat(log.search(null, LogEntryType.POS_ORDER_UNCHECKED, "539", null)).singleElement()
                .satisfies(entry -> {
                    assertThat(entry.item()).isEqualTo("Som tam");
                    assertThat(entry.memberName()).isEqualTo("Misha");
                    assertThat(entry.allergen()).isEqualTo(Allergen.PEANUTS);
                    assertThat(entry.severity()).isEqualTo(Severity.SEVERE);
                    assertThat(entry.outletName()).isEqualTo("Pool Bar");
                    assertThat(entry.statement())
                            .isEqualTo("Not checked at ordering: Som tam — PEANUTS for Misha (severe)");
                });
    }

    @Test
    void shouldNotLogPosRoundWithoutAllergensAtTheTable() {
        KitchenTicket ticket = kitchen.recordPosRound(round("3", KUZNETSOV,
                RoundLine.fromPos("Spring rolls", 2, List.of(), Set.of(Allergen.GLUTEN)))).orElseThrow();

        assertThat(ticket.hasAllergens()).isFalse();
        assertThat(log.search(null, LogEntryType.POS_ORDER_UNCHECKED, null, null)).isEmpty();
    }

    @Test
    void shouldRecordEachRoundOnceAndLetTheTabletReplaceTheFeedsTicket() {
        Round fromFeed = round("4", KUZNETSOV, RoundLine.fromPos("Kids nuggets", 1, List.of(), Set.of()));
        kitchen.recordPosRound(fromFeed);

        assertThat(kitchen.recordPosRound(fromFeed)).isEmpty();
        kitchen.recordCheckedRound(round("4", KUZNETSOV,
                new RoundLine("Kids nuggets", 1, List.of(), Set.of(), mikhail, false, null)));

        assertThat(kitchen.tickets(4)).singleElement().satisfies(ticket -> {
            assertThat(ticket.source()).isEqualTo(TicketSource.CHECKED);
            assertThat(ticket.lines().get(0).forMemberName()).isEqualTo("Mikhail");
        });
    }

    @Test
    void shouldShowAllergensOnlyForCheckWithoutReservation() {
        KitchenTicket ticket = kitchen.recordPosRound(round("5", null,
                RoundLine.fromPos("Pad thai", 1, List.of("Extra peanuts"), Set.of(Allergen.PEANUTS)))).orElseThrow();

        assertThat(ticket.source()).isEqualTo(TicketSource.UNLINKED);
        assertThat(ticket.hasAllergens()).isTrue();
        assertThat(ticket.lines().get(0).verdict()).isEqualTo(Verdict.UNKNOWN);
        assertThat(ticket.lines().get(0).condiments()).containsExactly("Extra peanuts");
    }

    @Test
    void shouldStillNotifyTheKitchenWhenThePmsFails() {
        when(pms.reservation("R-000001")).thenThrow(
                new SandboxException(SandboxErrorCode.SERVICE_UNAVAILABLE, 503, "down", null));

        KitchenTicket ticket = kitchen.recordPosRound(round("6", "R-000001",
                RoundLine.fromPos("Som tam", 1, List.of(), Set.of(Allergen.PEANUTS)))).orElseThrow();

        assertThat(ticket.source()).isEqualTo(TicketSource.UNCHECKED);
        assertThat(ticket.lines().get(0).verdict()).isEqualTo(Verdict.UNKNOWN);
        assertThat(ticket.lines().get(0).allergens()).containsExactly(Allergen.PEANUTS);
    }

    private static Round round(String checkSeq, String reservationId, RoundLine... lines) {
        return new Round(4, "Pool Bar", "1101", checkSeq, 1, "P3", reservationId, NOW, List.of(lines));
    }
}
