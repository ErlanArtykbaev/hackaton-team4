package com.hackathon.team4.allergen;

import static com.hackathon.team4.allergen.Allergen.EGGS;
import static com.hackathon.team4.allergen.Allergen.GLUTEN;
import static com.hackathon.team4.allergen.Allergen.MILK;
import static com.hackathon.team4.allergen.Allergen.PEANUTS;
import static com.hackathon.team4.allergen.Allergen.SOYBEANS;
import static com.hackathon.team4.allergen.Severity.INTOLERANCE;
import static com.hackathon.team4.allergen.Severity.SEVERE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** The Kuznetsov family of the demo: Mikhail (no allergies), Anna (milk intolerance), Misha (peanuts, severe). */
class AllergenEngineTest {

    private static final PartyMember MIKHAIL = new PartyMember(UUID.randomUUID(), "Mikhail", Map.of());
    private static final PartyMember ANNA = new PartyMember(UUID.randomUUID(), "Anna", Map.of(MILK, INTOLERANCE));
    private static final PartyMember MISHA = new PartyMember(UUID.randomUUID(), "Misha", Map.of(PEANUTS, SEVERE));
    private static final List<PartyMember> FAMILY = List.of(MIKHAIL, ANNA, MISHA);

    private final AllergenEngine engine = new AllergenEngine();

    static Stream<Arguments> itemVerdicts() {
        return Stream.of(
                Arguments.of("no allergens", Set.of(), Verdict.SAFE),
                Arguments.of("allergen nobody has", Set.of(GLUTEN, EGGS), Verdict.SAFE),
                Arguments.of("intolerance only", Set.of(MILK), Verdict.WARNING),
                Arguments.of("severe allergy", Set.of(PEANUTS, SOYBEANS), Verdict.DANGER),
                Arguments.of("severe beats intolerance", Set.of(PEANUTS, MILK), Verdict.DANGER));
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("itemVerdicts")
    void shouldGiveVerdictByWorstSeverityAtTheTable(String name, Set<Allergen> allergens, Verdict expected) {
        assertThat(engine.checkItem(FAMILY, allergens).verdict()).isEqualTo(expected);
    }

    @Test
    void shouldNameEveryMemberAndAllergenAtRisk() {
        ItemCheck check = engine.checkItem(FAMILY, Set.of(PEANUTS, MILK, GLUTEN));

        assertThat(check.dangerFor()).containsExactly(
                new DangerFor(ANNA.id(), "Anna", MILK, INTOLERANCE),
                new DangerFor(MISHA.id(), "Misha", PEANUTS, SEVERE));
    }

    @Test
    void shouldGiveUnknownWithoutMatchingForWalkIn() {
        ItemCheck check = engine.checkItem(List.of(), Set.of(PEANUTS));

        assertThat(check.verdict()).isEqualTo(Verdict.UNKNOWN);
        assertThat(check.dangerFor()).isEmpty();
    }

    @Test
    void shouldFlagEspressoWhenGuestIsAllergicToChosenCondiment() {
        Set<Allergen> espressoWithWholeMilk = AllergenEngine.withCondiments(Set.of(), List.of(Set.of(MILK)));

        ItemCheck check = engine.checkItem(FAMILY, espressoWithWholeMilk);

        assertThat(check.verdict()).isEqualTo(Verdict.WARNING);
        assertThat(check.dangerFor()).extracting(DangerFor::memberName).containsExactly("Anna");
    }

    @Test
    void shouldMergeItemAndCondimentAllergens() {
        assertThat(AllergenEngine.withCondiments(Set.of(GLUTEN), List.of(Set.of(MILK), Set.of(EGGS, GLUTEN))))
                .containsExactlyInAnyOrder(GLUTEN, MILK, EGGS);
    }

    static Stream<Arguments> lineRequirements() {
        return Stream.of(
                // nobody at the table allergic
                Arguments.of("safe dish for Misha", Set.of(GLUTEN), MISHA, Requirement.NONE, Verdict.SAFE),
                Arguments.of("safe dish for the table", Set.of(GLUTEN), null, Requirement.NONE, Verdict.SAFE),
                // for an allergic member
                Arguments.of("satay for Misha", Set.of(PEANUTS), MISHA, Requirement.GUEST_CONSENT, Verdict.DANGER),
                Arguments.of("iced tea for Anna", Set.of(MILK), ANNA, Requirement.GUEST_CONSENT, Verdict.WARNING),
                // for the table while someone is allergic
                Arguments.of("satay for the table", Set.of(PEANUTS), null, Requirement.GUEST_CONSENT, Verdict.DANGER),
                Arguments.of("iced tea for the table", Set.of(MILK), null, Requirement.GUEST_CONSENT,
                        Verdict.WARNING),
                // for a safe member while another is allergic
                Arguments.of("satay for Mikhail", Set.of(PEANUTS), MIKHAIL, Requirement.WAITER_CONFIRMATION,
                        Verdict.DANGER),
                Arguments.of("iced tea for Misha", Set.of(MILK), MISHA, Requirement.WAITER_CONFIRMATION,
                        Verdict.WARNING),
                // one allergen hits the member themself, another someone else: consent wins
                Arguments.of("peanut milk for Anna", Set.of(PEANUTS, MILK), ANNA, Requirement.GUEST_CONSENT,
                        Verdict.DANGER));
    }

    @ParameterizedTest(name = "{0} -> {3}")
    @MethodSource("lineRequirements")
    void shouldRequireWhatTheRulesSayForWhom(String name, Set<Allergen> allergens, PartyMember forMember,
            Requirement requirement, Verdict verdict) {
        LineCheck check = engine.checkLine(FAMILY, allergens, forMember == null ? null : forMember.id());

        assertThat(check.requirement()).isEqualTo(requirement);
        assertThat(check.verdict()).isEqualTo(verdict);
    }

    @Test
    void shouldKeepEveryoneAtRiskOnLineForSafeMember() {
        LineCheck check = engine.checkLine(FAMILY, Set.of(PEANUTS), MIKHAIL.id());

        assertThat(check.dangerFor()).containsExactly(new DangerFor(MISHA.id(), "Misha", PEANUTS, SEVERE));
    }

    @Test
    void shouldRequireNothingForWalkIn() {
        LineCheck check = engine.checkLine(List.of(), Set.of(PEANUTS), null);

        assertThat(check.verdict()).isEqualTo(Verdict.UNKNOWN);
        assertThat(check.requirement()).isEqualTo(Requirement.NONE);
        assertThat(check.dangerFor()).isEmpty();
    }

    @Test
    void shouldRefuseLineForMemberOutsideTheParty() {
        UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> engine.checkLine(FAMILY, Set.of(PEANUTS), stranger))
                .isInstanceOf(UnknownPartyMemberException.class);
    }

    private static final MenuCandidate SATAY = new MenuCandidate("SATAY", "MAINS", Set.of(PEANUTS, SOYBEANS));
    private static final MenuCandidate PAD_THAI = new MenuCandidate("PAD_THAI", "MAINS", Set.of(PEANUTS, EGGS));
    private static final MenuCandidate SPRING_ROLLS = new MenuCandidate("SPRING_ROLLS", "MAINS", Set.of(GLUTEN));
    private static final MenuCandidate CURRY = new MenuCandidate("CURRY", "MAINS", Set.of(MILK));
    private static final MenuCandidate NUGGETS = new MenuCandidate("NUGGETS", "KIDS", Set.of(GLUTEN, EGGS));
    private static final MenuCandidate RICE = new MenuCandidate("RICE", "MAINS", null);
    private static final List<MenuCandidate> MENU = List.of(SATAY, PAD_THAI, SPRING_ROLLS, CURRY, NUGGETS, RICE);

    @Test
    void shouldSuggestSafeItemsOfSameGroupInMenuOrder() {
        assertThat(engine.safeAlternatives(MENU, FAMILY, SATAY)).containsExactly("SPRING_ROLLS", "RICE");
    }

    @Test
    void shouldSuggestAlternativesForIntoleranceToo() {
        assertThat(engine.safeAlternatives(MENU, FAMILY, CURRY)).containsExactly("SPRING_ROLLS", "RICE");
    }

    @Test
    void shouldSuggestNothingForSafeItem() {
        assertThat(engine.safeAlternatives(MENU, FAMILY, SPRING_ROLLS)).isEmpty();
    }

    @Test
    void shouldSuggestNothingForWalkIn() {
        assertThat(engine.safeAlternatives(MENU, List.of(), SATAY)).isEmpty();
    }
}
