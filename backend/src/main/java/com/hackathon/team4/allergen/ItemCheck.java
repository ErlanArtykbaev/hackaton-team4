package com.hackathon.team4.allergen;

import java.util.List;

/**
 * The verdict of an item for a party, and for whom it is dangerous.
 */
public record ItemCheck(Verdict verdict, List<DangerFor> dangerFor) {

    public ItemCheck {
        dangerFor = List.copyOf(dangerFor);
    }
}
