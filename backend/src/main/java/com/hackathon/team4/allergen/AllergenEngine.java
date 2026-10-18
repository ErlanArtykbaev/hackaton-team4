package com.hackathon.team4.allergen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Matches the allergens of a dish against a party's passport. Deterministic, stateless and never blocking: it says
 * who is at risk and what the line needs, the guest decides.
 *
 * <p>Callers pass the item's allergens <b>including the chosen condiments</b> ({@link #withCondiments}). An empty
 * party is a walk-in: verdict {@link Verdict#UNKNOWN}, nothing matched.
 */
@Component
public class AllergenEngine {

    /** The allergens of an item plus those of its chosen condiments. */
    public static Set<Allergen> withCondiments(Collection<Allergen> itemAllergens,
            Collection<? extends Collection<Allergen>> condimentAllergens) {
        Set<Allergen> all = EnumSet.noneOf(Allergen.class);
        all.addAll(itemAllergens);
        condimentAllergens.forEach(all::addAll);
        return all;
    }

    public ItemCheck checkItem(List<PartyMember> party, Collection<Allergen> itemAllergens) {
        if (party.isEmpty()) {
            return new ItemCheck(Verdict.UNKNOWN, List.of());
        }
        List<DangerFor> dangerFor = dangerFor(party, itemAllergens);
        return new ItemCheck(verdictOf(dangerFor), dangerFor);
    }

    /**
     * @param forMemberId the member the line is for, or {@code null} for the whole table
     * @throws UnknownPartyMemberException when {@code forMemberId} is not in the party
     */
    public LineCheck checkLine(List<PartyMember> party, Collection<Allergen> itemAllergens, UUID forMemberId) {
        if (party.isEmpty()) {
            return new LineCheck(Verdict.UNKNOWN, List.of(), Requirement.NONE);
        }
        PartyMember forMember = forMemberId == null ? null : member(party, forMemberId);
        List<DangerFor> dangerFor = dangerFor(party, itemAllergens);
        return new LineCheck(verdictOf(dangerFor), dangerFor, requirementOf(dangerFor, forMember));
    }

    /**
     * Codes of the other items of the same group that are safe for the whole party, in menu order. Empty when the
     * item itself is safe or the party is unknown.
     */
    public List<String> safeAlternatives(List<MenuCandidate> menu, List<PartyMember> party, MenuCandidate item) {
        Verdict itemVerdict = checkItem(party, item.allergens()).verdict();
        if (itemVerdict == Verdict.SAFE || itemVerdict == Verdict.UNKNOWN) {
            return List.of();
        }
        return menu.stream()
                .filter(candidate -> candidate.group() != null && candidate.group().equals(item.group()))
                .filter(candidate -> !candidate.code().equals(item.code()))
                .filter(candidate -> checkItem(party, candidate.allergens()).verdict() == Verdict.SAFE)
                .map(MenuCandidate::code)
                .toList();
    }

    private static List<DangerFor> dangerFor(List<PartyMember> party, Collection<Allergen> itemAllergens) {
        Set<Allergen> allergens = itemAllergens.isEmpty() ? Set.of() : EnumSet.copyOf(itemAllergens);
        List<DangerFor> dangerFor = new ArrayList<>();
        for (PartyMember member : party) {
            for (Allergen allergen : allergens) {
                if (member.allergicTo(allergen)) {
                    dangerFor.add(new DangerFor(member.id(), member.name(), allergen,
                            member.allergies().get(allergen)));
                }
            }
        }
        return dangerFor;
    }

    private static Verdict verdictOf(List<DangerFor> dangerFor) {
        if (dangerFor.stream().anyMatch(danger -> danger.severity() == Severity.SEVERE)) {
            return Verdict.DANGER;
        }
        return dangerFor.isEmpty() ? Verdict.SAFE : Verdict.WARNING;
    }

    private static Requirement requirementOf(List<DangerFor> dangerFor, PartyMember forMember) {
        if (dangerFor.isEmpty()) {
            return Requirement.NONE;
        }
        if (forMember == null || dangerFor.stream().anyMatch(danger -> danger.memberId().equals(forMember.id()))) {
            return Requirement.GUEST_CONSENT;
        }
        return Requirement.WAITER_CONFIRMATION;
    }

    private static PartyMember member(List<PartyMember> party, UUID memberId) {
        return party.stream()
                .filter(member -> member.id().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new UnknownPartyMemberException(memberId));
    }
}
