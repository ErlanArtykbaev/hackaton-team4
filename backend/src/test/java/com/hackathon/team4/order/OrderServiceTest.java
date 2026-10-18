package com.hackathon.team4.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.AllergenEngine;
import com.hackathon.team4.allergen.PartyMember;
import com.hackathon.team4.allergen.Requirement;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.kitchen.KitchenTicketService;
import com.hackathon.team4.menu.MenuService;
import com.hackathon.team4.menu.OutletService;
import com.hackathon.team4.menu.dto.CondimentGroup;
import com.hackathon.team4.menu.dto.CondimentOption;
import com.hackathon.team4.menu.dto.MenuItem;
import com.hackathon.team4.order.OrderRejectedException.Reason;
import com.hackathon.team4.order.dto.ConsentRequest;
import com.hackathon.team4.order.dto.LineCheck;
import com.hackathon.team4.order.dto.OrderLineRequest;
import com.hackathon.team4.order.dto.OrderRequest;
import com.hackathon.team4.passport.Party;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.safetylog.SafetyLogService;
import com.hackathon.team4.sandbox.pos.PosGateway;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** What each order line needs ({@code PROJECT-CONTEXT.md} §3), condiments included, and that it is enforced. */
class OrderServiceTest {

    private static final int POOL_BAR = 4;
    private static final String RESERVATION = "R-990002";
    private static final PartyMember MIKHAIL = new PartyMember(UUID.randomUUID(), "Mikhail", Map.of());
    private static final PartyMember ANNA = new PartyMember(UUID.randomUUID(), "Anna",
            Map.of(Allergen.MILK, Severity.INTOLERANCE));
    private static final PartyMember MISHA = new PartyMember(UUID.randomUUID(), "Misha",
            Map.of(Allergen.PEANUTS, Severity.SEVERE));

    private final MenuService menus = mock(MenuService.class);
    private final PassportService passports = mock(PassportService.class);
    private final PosGateway pos = mock(PosGateway.class);
    private final OrderService orders = new OrderService(menus, mock(OutletService.class), passports,
            new AllergenEngine(), pos, mock(KitchenTicketService.class), mock(SafetyLogService.class),
            Clock.systemUTC());

    @BeforeEach
    void poolBarMenuAndKuznetsovParty() {
        when(menus.menu(POOL_BAR, null)).thenReturn(List.of(
                item("1019", "Chicken satay", List.of(Allergen.PEANUTS, Allergen.SOYBEANS), List.of()),
                item("1036", "Kids nuggets", List.of(Allergen.GLUTEN, Allergen.EGGS), List.of()),
                item("2018", "Espresso", List.of(), List.of(new CondimentGroup("MILK", "Milk", 0, 1, List.of(
                        option("7021", "Whole milk", Allergen.MILK),
                        option("7022", "Oat milk", Allergen.GLUTEN)))))));
        when(passports.party(RESERVATION)).thenReturn(new Party(RESERVATION, "518", "Mikhail Kuznetsov",
                List.of(MIKHAIL, ANNA, MISHA)));
    }

    @Test
    void shouldAskConsentWhenTheChosenCondimentContainsTheGuestsAllergen() {
        List<LineCheck> lines = orders.check(order(RESERVATION,
                line("2018", List.of("7021"), ANNA.id(), false, null),
                line("2018", List.of("7022"), ANNA.id(), false, null))).lines();

        assertThat(lines.get(0).verdict()).isEqualTo(Verdict.WARNING);
        assertThat(lines.get(0).requirement()).isEqualTo(Requirement.GUEST_CONSENT);
        assertThat(lines.get(0).dangerFor()).singleElement()
                .satisfies(danger -> assertThat(danger.allergen()).isEqualTo(Allergen.MILK));
        assertThat(lines.get(1).verdict()).isEqualTo(Verdict.SAFE);
        assertThat(lines.get(1).requirement()).isEqualTo(Requirement.NONE);
    }

    @Test
    void shouldAskConfirmationForASafeMemberAndConsentForTheTable() {
        List<LineCheck> lines = orders.check(order(RESERVATION,
                line("1019", List.of(), MIKHAIL.id(), false, null),
                line("1019", List.of(), null, false, null),
                line("1036", List.of(), MISHA.id(), false, null))).lines();

        assertThat(lines).extracting(LineCheck::verdict, LineCheck::requirement).containsExactly(
                tuple(Verdict.DANGER, Requirement.WAITER_CONFIRMATION),
                tuple(Verdict.DANGER, Requirement.GUEST_CONSENT),
                tuple(Verdict.SAFE, Requirement.NONE));
    }

    @Test
    void shouldRefuseToSendALineWithoutTheWaitersConfirmation() {
        OrderRequest order = order(RESERVATION, line("1019", List.of(), MIKHAIL.id(), false, null));

        assertThatThrownBy(() -> orders.place(order)).isInstanceOfSatisfying(OrderRejectedException.class,
                rejected -> assertThat(rejected.reason()).isEqualTo(Reason.WAITER_CONFIRMATION_REQUIRED));
    }

    @Test
    void shouldRefuseToSendALineWithoutTheGuestsConsent() {
        OrderRequest order = order(RESERVATION,
                line("1019", List.of(), MIKHAIL.id(), true, null),
                line("2018", List.of("7021"), ANNA.id(), false, null));

        assertThatThrownBy(() -> orders.place(order)).isInstanceOfSatisfying(OrderRejectedException.class,
                rejected -> assertThat(rejected.reason()).isEqualTo(Reason.GUEST_CONSENT_REQUIRED));
        verifyNoInteractions(pos);
    }

    @Test
    void shouldMatchNothingForWalkIn() {
        List<LineCheck> lines = orders.check(order(null, line("1019", List.of(), null, false, null))).lines();

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.verdict()).isEqualTo(Verdict.UNKNOWN);
            assertThat(line.requirement()).isEqualTo(Requirement.NONE);
        });
        verify(passports, never()).party(anyString());
    }

    @Test
    void shouldRejectAnItemCondimentOrMemberThatDoesNotExist() {
        assertRejected(order(RESERVATION, line("9999", List.of(), null, false, null)));
        assertRejected(order(RESERVATION, line("1019", List.of("7021"), null, false, null)));
        assertRejected(order(RESERVATION, line("1036", List.of(), UUID.randomUUID(), false, null)));
    }

    private void assertRejected(OrderRequest order) {
        assertThatThrownBy(() -> orders.check(order)).isInstanceOfSatisfying(OrderRejectedException.class,
                rejected -> assertThat(rejected.reason()).isEqualTo(Reason.VALIDATION_ERROR));
    }

    private static OrderRequest order(String reservationId, OrderLineRequest... lines) {
        return new OrderRequest(POOL_BAR, reservationId, "P3", "W02", List.of(lines));
    }

    private static OrderLineRequest line(String code, List<String> condiments, UUID forMemberId,
            boolean waiterConfirmed, ConsentRequest consent) {
        return new OrderLineRequest(code, 1, condiments, forMemberId, waiterConfirmed, consent);
    }

    private static MenuItem item(String code, String name, List<Allergen> allergens, List<CondimentGroup> groups) {
        return new MenuItem(code, name, "Group", BigDecimal.TEN, allergens, Verdict.UNKNOWN, List.of(), List.of(),
                groups);
    }

    private static CondimentOption option(String code, String name, Allergen allergen) {
        return new CondimentOption(code, name, BigDecimal.ZERO, List.of(allergen), Verdict.UNKNOWN, List.of());
    }
}
