package com.hackathon.team4.allergen;

import java.util.Set;

/**
 * A menu item as the engine needs it to suggest alternatives: its code, its group and its own allergens.
 */
public record MenuCandidate(String code, String group, Set<Allergen> allergens) {

    public MenuCandidate {
        allergens = allergens == null ? Set.of() : Set.copyOf(allergens);
    }
}
