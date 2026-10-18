package com.hackathon.team4.kitchen;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.kitchen.dto.KitchenTicketLine;
import com.hackathon.team4.kitchen.dto.KitchenTicketResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Kitchen tickets of an outlet, newest first.
 *
 * <p>Stub (BE-A2): fixed tickets from the demo story at the Pool Bar, one per source; BE-B4 replaces them with the
 * stored tickets.
 */
@Service
public class KitchenTicketService {

    private static final DangerFor MISHA_PEANUTS = new DangerFor(
            UUID.fromString("518a0000-0000-4000-8000-000000000003"), "Misha", Allergen.PEANUTS, Severity.SEVERE);
    private static final DangerFor ANNA_MILK = new DangerFor(
            UUID.fromString("518a0000-0000-4000-8000-000000000002"), "Anna", Allergen.MILK, Severity.INTOLERANCE);

    private static final List<KitchenTicketResponse> TICKETS = List.of(
            new KitchenTicketResponse(UUID.fromString("4a000000-0000-4000-8000-000000000003"), 4, 1018, "P5",
                    "518", "Mikhail Kuznetsov", at(18, 50), TicketSource.UNCHECKED, true, List.of(
                    new KitchenTicketLine("Som tam", 1, List.of(), List.of(Allergen.PEANUTS, Allergen.FISH),
                            Verdict.DANGER, List.of(MISHA_PEANUTS), null, false, null))),
            new KitchenTicketResponse(UUID.fromString("4a000000-0000-4000-8000-000000000002"), 4, 1017, "P3",
                    "518", "Mikhail Kuznetsov", at(18, 45), TicketSource.CHECKED, true, List.of(
                    new KitchenTicketLine("Kids nuggets", 1, List.of(), List.of(Allergen.GLUTEN, Allergen.EGGS),
                            Verdict.SAFE, List.of(), "Misha", false, null),
                    new KitchenTicketLine("Coconut ice cream", 1, List.of(), List.of(),
                            Verdict.SAFE, List.of(), "Misha", false, null),
                    new KitchenTicketLine("Chicken satay", 1, List.of(), List.of(Allergen.PEANUTS),
                            Verdict.DANGER, List.of(MISHA_PEANUTS), "Mikhail", true, null),
                    new KitchenTicketLine("Thai iced tea", 1, List.of(), List.of(Allergen.MILK),
                            Verdict.WARNING, List.of(ANNA_MILK), "Anna", false, "Anna Kuznetsova"),
                    new KitchenTicketLine("Espresso", 1, List.of("Oat milk"), List.of(),
                            Verdict.SAFE, List.of(), "Anna", false, null))),
            new KitchenTicketResponse(UUID.fromString("4a000000-0000-4000-8000-000000000001"), 4, 1016, "P1",
                    null, null, at(18, 30), TicketSource.UNLINKED, true, List.of(
                    new KitchenTicketLine("Pad thai", 2, List.of(),
                            List.of(Allergen.PEANUTS, Allergen.EGGS, Allergen.FISH),
                            Verdict.UNKNOWN, List.of(), null, false, null),
                    new KitchenTicketLine("Coconut ice cream", 1, List.of(), List.of(),
                            Verdict.UNKNOWN, List.of(), null, false, null))));

    public List<KitchenTicketResponse> tickets(int rvc) {
        return TICKETS.stream().filter(ticket -> ticket.rvc() == rvc).toList();
    }

    private static OffsetDateTime at(int hour, int minute) {
        return OffsetDateTime.of(2026, 10, 18, hour, minute, 0, 0, ZoneOffset.ofHours(7));
    }
}
