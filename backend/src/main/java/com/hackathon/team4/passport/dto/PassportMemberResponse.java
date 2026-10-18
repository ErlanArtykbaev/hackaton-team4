package com.hackathon.team4.passport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * A card of the passport, one per person. {@code id} is {@code null} for the primary guest of a reservation
 * without a passport yet: the member is created on the first save. {@code age} is set for children.
 */
@Schema(name = "PassportMember")
public record PassportMemberResponse(
        UUID id,
        String name,
        Integer age,
        boolean primary,
        List<MemberAllergyResponse> allergies) {
}
