package com.hackathon.team4.order.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The guest's consent to a line with their allergen, given on the tablet with their name; for a child, a parent's.
 */
public record ConsentRequest(@NotBlank String givenBy) {
}
