package com.hackathon.team4.passport.dto;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.passport.PassportSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * The allergy passport of a reservation. {@code pmsAllergens} are the allergies on the PMS profile: the union of
 * all members' allergens once the passport is saved. {@code updatedAt} and {@code updatedBy} are {@code null}
 * until the first save.
 */
@Schema(name = "Passport")
public record PassportResponse(
        String reservationId,
        String roomNumber,
        String guestName,
        String notes,
        List<Allergen> pmsAllergens,
        List<PassportMemberResponse> members,
        OffsetDateTime updatedAt,
        String updatedBy) {

    public static PassportResponse from(PassportSnapshot passport) {
        List<PassportMemberResponse> members = passport.members().stream()
                .map(PassportMemberResponse::from)
                .toList();
        OffsetDateTime updatedAt = passport.updatedAt() == null ? null : passport.updatedAt().atOffset(ZoneOffset.UTC);
        return new PassportResponse(passport.reservationId(), passport.roomNumber(), passport.guestName(),
                passport.notes(), passport.pmsAllergens(), members, updatedAt, passport.updatedBy());
    }
}
