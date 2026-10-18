package com.hackathon.team4.allergy;

import java.util.UUID;

/**
 * One member for whom an item is dangerous, and why.
 */
public record DangerFor(UUID memberId, String memberName, Allergen allergen, Severity severity) {
}
