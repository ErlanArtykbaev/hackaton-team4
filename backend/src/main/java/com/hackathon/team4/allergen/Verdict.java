package com.hackathon.team4.allergen;

/**
 * What a menu item or an order line means for a party.
 */
public enum Verdict {
    /** Nobody in the party is allergic to it. */
    SAFE,
    /** Contains an allergen of a member with an intolerance. */
    WARNING,
    /** Contains an allergen of a member with a severe allergy. */
    DANGER,
    /** No reservation (walk-in): allergens are shown, nothing is matched. */
    UNKNOWN
}
