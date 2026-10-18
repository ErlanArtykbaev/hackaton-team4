package com.hackathon.team4.passport;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.passport.dto.MemberAllergyResponse;
import com.hackathon.team4.passport.dto.PassportMemberRequest;
import com.hackathon.team4.passport.dto.PassportMemberResponse;
import com.hackathon.team4.passport.dto.PassportResponse;
import com.hackathon.team4.passport.dto.PassportUpdateRequest;
import com.hackathon.team4.sandbox.SandboxErrorCode;
import com.hackathon.team4.sandbox.SandboxException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Allergy passports: a card per family member, the PMS profile keeps the union of their allergens.
 *
 * <p>Stub (BE-A2): fixed data from the demo story and nothing is stored; BE-B3 replaces it with the passport
 * storage, the default from the PMS profile and the PMS sync.
 */
@Service
public class PassportService {

    private static final String KUZNETSOV = "R-203844";
    private static final String THOMAS = "R-203981";

    /** The Kuznetsov family after the waiter added Anna's milk intolerance (demo step 2). */
    private static final PassportResponse KUZNETSOV_PASSPORT = new PassportResponse(
            KUZNETSOV, "518", "Mikhail Kuznetsov", null,
            List.of(Allergen.PEANUTS, Allergen.MILK),
            List.of(
                    new PassportMemberResponse(UUID.fromString("518a0000-0000-4000-8000-000000000001"),
                            "Mikhail", null, true, List.of()),
                    new PassportMemberResponse(UUID.fromString("518a0000-0000-4000-8000-000000000002"),
                            "Anna", null, false,
                            List.of(new MemberAllergyResponse(Allergen.MILK, Severity.INTOLERANCE))),
                    new PassportMemberResponse(UUID.fromString("518a0000-0000-4000-8000-000000000003"),
                            "Misha", 7, false,
                            List.of(new MemberAllergyResponse(Allergen.PEANUTS, Severity.SEVERE)))),
            OffsetDateTime.of(2026, 10, 18, 18, 42, 0, 0, ZoneOffset.ofHours(7)), "W02");

    /** No passport yet: the primary guest with the PMS allergens as severe. */
    private static final PassportResponse THOMAS_DEFAULT = new PassportResponse(
            THOMAS, "729", "Oliver Thomas", null,
            List.of(Allergen.GLUTEN, Allergen.FISH, Allergen.MUSTARD),
            List.of(new PassportMemberResponse(null, "Oliver Thomas", null, true, List.of(
                    new MemberAllergyResponse(Allergen.GLUTEN, Severity.SEVERE),
                    new MemberAllergyResponse(Allergen.FISH, Severity.SEVERE),
                    new MemberAllergyResponse(Allergen.MUSTARD, Severity.SEVERE)))),
            null, null);

    private final Clock clock;

    public PassportService(Clock clock) {
        this.clock = clock;
    }

    public PassportResponse passport(String reservationId) {
        return switch (reservationId) {
            case KUZNETSOV -> KUZNETSOV_PASSPORT;
            case THOMAS -> THOMAS_DEFAULT;
            default -> throw new SandboxException(SandboxErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND.value(),
                    "No reservation " + reservationId, null);
        };
    }

    /** Returns the saved members, new ones with an id, and the PMS allergens as their union; stores nothing. */
    public PassportResponse update(String reservationId, PassportUpdateRequest update) {
        PassportResponse current = passport(reservationId);
        List<PassportMemberResponse> members = update.members().stream().map(PassportService::member).toList();
        List<Allergen> pmsAllergens = members.stream()
                .flatMap(member -> member.allergies().stream())
                .map(MemberAllergyResponse::allergen)
                .distinct()
                .sorted()
                .toList();
        return new PassportResponse(reservationId, current.roomNumber(), current.guestName(), current.notes(),
                pmsAllergens, members, OffsetDateTime.now(clock), update.updatedBy());
    }

    private static PassportMemberResponse member(PassportMemberRequest request) {
        UUID id = request.id() == null ? UUID.randomUUID() : request.id();
        List<MemberAllergyResponse> allergies = request.allergies().stream()
                .map(allergy -> new MemberAllergyResponse(allergy.allergen(), allergy.severity()))
                .toList();
        return new PassportMemberResponse(id, request.name(), request.age(), request.primary(), allergies);
    }
}
