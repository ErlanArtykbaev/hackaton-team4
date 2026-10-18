package com.hackathon.team4.passport.dto;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import jakarta.validation.constraints.NotNull;

/**
 * One allergy on a member's card, as the front desk or the waiter enters it.
 */
public record MemberAllergyRequest(@NotNull Allergen allergen, @NotNull Severity severity) {
}
