package com.hackathon.team4.allergen;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A person at the table as the engine sees them: who they are and what they are allergic to.
 */
public record PartyMember(UUID id, String name, Map<Allergen, Severity> allergies) {

    public PartyMember {
        Objects.requireNonNull(id);
        allergies = allergies == null ? Map.of() : Map.copyOf(allergies);
    }

    public boolean allergicTo(Allergen allergen) {
        return allergies.containsKey(allergen);
    }
}
