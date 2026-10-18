package com.hackathon.team4.passport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.UUID;

/**
 * A card to save; {@code id} is {@code null} for a new member.
 */
public record PassportMemberRequest(
        UUID id,
        @NotBlank String name,
        @PositiveOrZero Integer age,
        boolean primary,
        @NotNull @Valid List<MemberAllergyRequest> allergies) {
}
