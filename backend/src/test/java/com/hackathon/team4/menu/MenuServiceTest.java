package com.hackathon.team4.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.AllergenEngine;
import com.hackathon.team4.allergen.PartyMember;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.menu.dto.CondimentOption;
import com.hackathon.team4.menu.dto.MenuItem;
import com.hackathon.team4.passport.Party;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.sandbox.SandboxStubs;
import com.hackathon.team4.sandbox.generated.model.MenuListResponse;
import com.hackathon.team4.sandbox.pos.PosGateway;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verdicts on the POS menu for a party, the walk-in menu and the 5-minute menu cache. */
class MenuServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");
    private static final String RESERVATION = "R-990001";
    private static final PartyMember ANNA = new PartyMember(UUID.randomUUID(), "Anna",
            Map.of(Allergen.MILK, Severity.INTOLERANCE));
    private static final PartyMember MISHA = new PartyMember(UUID.randomUUID(), "Misha",
            Map.of(Allergen.NUTS, Severity.SEVERE));

    private final PosGateway pos = mock(PosGateway.class);
    private final PassportService passports = mock(PassportService.class);
    private final Clock clock = mock(Clock.class);
    private final MenuService menus = new MenuService(pos, passports, new AllergenEngine(), clock);

    @BeforeEach
    void posMenuAndParty() throws Exception {
        MenuListResponse menu = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(SandboxStubs.fixture("pos/menu-2"), MenuListResponse.class);
        when(pos.menu(2, "en")).thenReturn(menu.getElements());
        when(passports.party(RESERVATION)).thenReturn(new Party(RESERVATION, "633", "Anna Kuznetsova",
                List.of(ANNA, MISHA)));
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void shouldMarkItemsDangerousForWhomWithSafeAlternativesInTheSameGroup() {
        MenuItem badrijani = item(menus.menu(2, RESERVATION), "1020");

        assertThat(badrijani.group()).isEqualTo("Starters");
        assertThat(badrijani.verdict()).isEqualTo(Verdict.DANGER);
        assertThat(badrijani.dangerFor()).singleElement().satisfies(danger -> {
            assertThat(danger.memberName()).isEqualTo("Misha");
            assertThat(danger.allergen()).isEqualTo(Allergen.NUTS);
        });
        assertThat(badrijani.safeAlternatives()).containsExactly("1023");
        assertThat(item(menus.menu(2, RESERVATION), "1026").verdict()).isEqualTo(Verdict.WARNING);
        assertThat(item(menus.menu(2, RESERVATION), "1023").verdict()).isEqualTo(Verdict.SAFE);
    }

    @Test
    void shouldGiveEachCondimentItsOwnVerdict() {
        MenuItem espresso = item(menus.menu(2, RESERVATION), "2014");

        assertThat(espresso.verdict()).isEqualTo(Verdict.SAFE);
        List<CondimentOption> milks = espresso.condimentGroups().get(0).options();
        assertThat(milks).extracting(CondimentOption::code, CondimentOption::verdict).containsExactly(
                tuple("7021", Verdict.WARNING),
                tuple("7022", Verdict.SAFE),
                tuple("7023", Verdict.DANGER),
                tuple("7024", Verdict.SAFE));
        assertThat(milks.get(0).dangerFor()).singleElement()
                .satisfies(danger -> assertThat(danger.memberName()).isEqualTo("Anna"));
    }

    @Test
    void shouldShowAllergensOnlyForWalkIn() {
        List<MenuItem> menu = menus.menu(2, null);

        assertThat(menu).allSatisfy(item -> {
            assertThat(item.verdict()).isEqualTo(Verdict.UNKNOWN);
            assertThat(item.dangerFor()).isEmpty();
            assertThat(item.safeAlternatives()).isEmpty();
        });
        assertThat(item(menu, "1020").allergens()).containsExactly(Allergen.NUTS);
        assertThat(item(menu, "2014").condimentGroups().get(0).options())
                .allSatisfy(option -> assertThat(option.verdict()).isEqualTo(Verdict.UNKNOWN));
        verify(passports, never()).party(anyString());
    }

    @Test
    void shouldLeaveDiscountsOutOfTheMenu() {
        assertThat(menus.menu(2, null)).extracting(MenuItem::code).doesNotContain("9001", "9010").contains("2014");
    }

    @Test
    void shouldReadThePosMenuAtMostOnceInFiveMinutes() {
        menus.menu(2, null);
        menus.menu(2, RESERVATION);
        when(clock.instant()).thenReturn(NOW.plus(MenuService.MENU_TTL).minusSeconds(1));
        menus.menu(2, null);
        verify(pos, times(1)).menu(2, "en");

        when(clock.instant()).thenReturn(NOW.plus(MenuService.MENU_TTL));
        menus.menu(2, null);
        verify(pos, times(2)).menu(2, "en");
    }

    private static MenuItem item(List<MenuItem> menu, String code) {
        return menu.stream().filter(item -> item.code().equals(code)).findFirst().orElseThrow();
    }
}
