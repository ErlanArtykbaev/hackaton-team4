package com.hackathon.team4.kitchen.dto;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.kitchen.TicketLine;
import java.util.List;

/**
 * One line of a kitchen ticket, its condiments' allergens included, with the waiter's confirmation and the
 * name the guest typed when consenting ({@code null} when none was needed).
 */
public record KitchenTicketLine(
        String name,
        int quantity,
        List<String> condiments,
        List<Allergen> allergens,
        Verdict verdict,
        List<DangerFor> dangerFor,
        String forMemberName,
        boolean waiterConfirmed,
        String consentBy) {

    public static KitchenTicketLine from(TicketLine line) {
        return new KitchenTicketLine(line.name(), line.quantity(), line.condiments(), line.allergens(),
                line.verdict(), line.dangerFor(), line.forMemberName(), line.waiterConfirmed(), line.consentBy());
    }
}
