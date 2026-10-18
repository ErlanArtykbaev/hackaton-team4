package com.hackathon.team4.passport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * A card of the passport, one per person. Without a saved passport the primary guest already has a stable
 * {@code id} and becomes the stored member on the first save. {@code age} is set for children.
 */
@Schema(name = "PassportMember")
public record PassportMemberResponse(
        UUID id,
        String name,
        Integer age,
        boolean primary,
        List<MemberAllergyResponse> allergies) {
}
