package com.hackathon.team4.passport.dto;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One allergy on a member's card.
 */
@Schema(name = "MemberAllergy")
public record MemberAllergyResponse(Allergen allergen, Severity severity) {
}
