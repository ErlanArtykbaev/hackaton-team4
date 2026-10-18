package com.hackathon.team4.menu.dto;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.Verdict;
import java.math.BigDecimal;
import java.util.List;

/**
 * One choice of a condiment group, with its own verdict for the party.
 */
public record CondimentOption(
        String code,
        String name,
        BigDecimal price,
        List<Allergen> allergens,
        Verdict verdict,
        List<DangerFor> dangerFor) {
}
