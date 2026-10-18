package com.hackathon.team4.allergen;

import java.util.List;

/**
 * The verdict of an order line for the whole table, and what the line needs before it is sent.
 */
public record LineCheck(Verdict verdict, List<DangerFor> dangerFor, Requirement requirement) {

    public LineCheck {
        dangerFor = List.copyOf(dangerFor);
    }
}
