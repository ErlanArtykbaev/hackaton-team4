package com.hackathon.team4.passport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.PartyMember;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogRepository;
import com.hackathon.team4.safetylog.SafetyLogService;
import com.hackathon.team4.sandbox.SandboxErrorCode;
import com.hackathon.team4.sandbox.SandboxException;
import com.hackathon.team4.sandbox.generated.model.Guest;
import com.hackathon.team4.sandbox.generated.model.GuestPatch;
import com.hackathon.team4.sandbox.generated.model.Reservation;
import com.hackathon.team4.sandbox.pms.PmsGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@ActiveProfiles("test")
@Import({PassportService.class, SafetyLogService.class, PassportServiceTest.FixedClock.class})
class PassportServiceTest {

    static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");
    private static final String RESERVATION = "R-203478";

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
    private PassportService passports;

    @Autowired
    private SafetyLogRepository log;

    @BeforeEach
    void reservationWithPeanutsOnThePmsProfile() {
        when(pms.reservation(RESERVATION)).thenReturn(reservation(
                List.of(com.hackathon.team4.sandbox.generated.model.Allergen.PEANUTS)));
    }

    @Test
    void shouldShowPrimaryGuestWithPmsAllergensAsSevereBeforeFirstSave() {
        PassportSnapshot snapshot = passports.get(RESERVATION);

        assertThat(snapshot.updatedAt()).isNull();
        assertThat(snapshot.pmsAllergens()).containsExactly(Allergen.PEANUTS);
        assertThat(snapshot.members()).singleElement().satisfies(member -> {
            assertThat(member.name()).isEqualTo("Mikhail Kuznetsov");
            assertThat(member.primary()).isTrue();
            assertThat(member.allergies()).containsExactlyEntriesOf(Map.of(Allergen.PEANUTS, Severity.SEVERE));
        });
        assertThat(passports.get(RESERVATION).members().get(0).id()).isEqualTo(snapshot.members().get(0).id());
    }

    @Test
    void shouldStoreCardsSetPmsToTheUnionAndLogTheChange() {
        UUID primaryId = passports.get(RESERVATION).members().get(0).id();

        PassportSnapshot saved = passports.save(RESERVATION, null, List.of(
                member(primaryId, "Mikhail", null, true, Map.of()),
                member(null, "Anna", null, false, Map.of(Allergen.MILK, Severity.INTOLERANCE)),
                member(null, "Misha", 7, false, Map.of(Allergen.PEANUTS, Severity.SEVERE))),
                "W02", PassportService.Source.WAITER, 4, "Pool Bar");

        ArgumentCaptor<GuestPatch> patch = ArgumentCaptor.forClass(GuestPatch.class);
        verify(pms).updateGuest(eq("G-203478"), patch.capture());
        assertThat(patch.getValue().getAllergies()).containsExactlyInAnyOrder(
                com.hackathon.team4.sandbox.generated.model.Allergen.MILK,
                com.hackathon.team4.sandbox.generated.model.Allergen.PEANUTS);
        assertThat(patch.getValue().getPreferences()).containsExactly("quiet room");
        assertThat(saved.members()).extracting(PassportSnapshot.Member::name)
                .containsExactly("Mikhail", "Anna", "Misha");
        assertThat(saved.members().get(0).id()).isEqualTo(primaryId);
        assertThat(saved.pmsAllergens()).containsExactlyInAnyOrder(Allergen.MILK, Allergen.PEANUTS);
        assertThat(log.search(null, LogEntryType.PASSPORT_UPDATED, "518", null)).singleElement()
                .satisfies(entry -> {
                    assertThat(entry.actor()).isEqualTo("W02");
                    assertThat(entry.outletName()).isEqualTo("Pool Bar");
                    assertThat(entry.guestName()).isEqualTo("Mikhail Kuznetsov");
                    assertThat(entry.statement()).isEqualTo(
                            "WAITER: Mikhail: -PEANUTS; added Anna; Anna: +MILK (intolerance); added Misha; "
                                    + "Misha: +PEANUTS (severe)");
                });
    }

    @Test
    void shouldNotPatchPmsWhenTheUnionIsUnchanged() {
        passports.save(RESERVATION, null, List.of(
                member(null, "Misha", 7, false, Map.of(Allergen.PEANUTS, Severity.INTOLERANCE))),
                "front-desk", PassportService.Source.FRONT_DESK, null, null);

        verify(pms, never()).updateGuest(anyString(), any());
    }

    @Test
    void shouldGiveTheEngineTheStoredMembers() {
        passports.save(RESERVATION, null, List.of(
                member(null, "Mikhail", null, true, Map.of()),
                member(null, "Misha", 7, false, Map.of(Allergen.PEANUTS, Severity.SEVERE))),
                "front-desk", PassportService.Source.FRONT_DESK, null, null);

        Party party = passports.party(RESERVATION);

        assertThat(party.roomNumber()).isEqualTo("518");
        assertThat(party.members()).extracting(PartyMember::name).containsExactly("Mikhail", "Misha");
        assertThat(party.members().get(1).allergies()).containsEntry(Allergen.PEANUTS, Severity.SEVERE);
    }

    @Test
    void shouldDescribeAddedChangedAndRemovedAllergies() {
        passports.save(RESERVATION, null, List.of(
                member(null, "Anna", null, true, Map.of(Allergen.MILK, Severity.INTOLERANCE, Allergen.EGGS,
                        Severity.INTOLERANCE)),
                member(null, "Oleg", null, false, Map.of())),
                "front-desk", PassportService.Source.FRONT_DESK, null, null);
        UUID anna = passports.get(RESERVATION).members().get(0).id();

        passports.save(RESERVATION, null, List.of(
                member(anna, "Anna", null, true, Map.of(Allergen.MILK, Severity.SEVERE))),
                "front-desk", PassportService.Source.FRONT_DESK, null, null);

        assertThat(log.search(null, LogEntryType.PASSPORT_UPDATED, null, null).get(0).statement())
                .isEqualTo("FRONT_DESK: Anna: +MILK (severe), -EGGS; removed Oleg");
    }

    @Test
    void shouldSaveNothingWhenThePmsRejectsTheUpdate() {
        when(pms.updateGuest(anyString(), any())).thenThrow(
                new SandboxException(SandboxErrorCode.SERVICE_UNAVAILABLE, 503, "down", null));

        assertThatThrownBy(() -> passports.save(RESERVATION, null, List.of(
                member(null, "Anna", null, true, Map.of(Allergen.MILK, Severity.INTOLERANCE))),
                "front-desk", PassportService.Source.FRONT_DESK, null, null))
                .isInstanceOf(SandboxException.class);
    }

    private static PassportSnapshot.Member member(UUID id, String name, Integer age, boolean primary,
            Map<Allergen, Severity> allergies) {
        return new PassportSnapshot.Member(id, name, age, primary, allergies);
    }

    private static Reservation reservation(List<com.hackathon.team4.sandbox.generated.model.Allergen> allergies) {
        return new Reservation().id(RESERVATION).roomNumber("518").guest(new Guest().id("G-203478")
                .firstName("Mikhail").lastName("Kuznetsov").allergies(allergies)
                .preferences(List.of("quiet room")).marketingConsent(false));
    }
}
