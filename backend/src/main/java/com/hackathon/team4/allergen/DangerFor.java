package com.hackathon.team4.allergen;

import java.util.UUID;

/**
 * A party member for whom a menu item or an order line contains one of their allergens.
 */
public record DangerFor(UUID memberId, String memberName, Allergen allergen, Severity severity) {
}
