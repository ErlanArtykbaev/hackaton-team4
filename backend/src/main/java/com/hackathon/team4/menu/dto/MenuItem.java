package com.hackathon.team4.menu.dto;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.Verdict;
import java.math.BigDecimal;
import java.util.List;

/**
 * A menu item of an outlet with its verdict for the party; prices in the hotel currency (THB).
 * {@code safeAlternatives} are codes of safe items in the same group.
 */
public record MenuItem(
        String code,
        String name,
        String group,
        BigDecimal price,
        List<Allergen> allergens,
        Verdict verdict,
        List<DangerFor> dangerFor,
        List<String> safeAlternatives,
        List<CondimentGroup> condimentGroups) {
}
