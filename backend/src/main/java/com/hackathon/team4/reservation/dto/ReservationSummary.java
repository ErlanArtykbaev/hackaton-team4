package com.hackathon.team4.reservation.dto;

import com.hackathon.team4.allergen.Allergen;
import java.time.LocalDate;
import java.util.List;

/**
 * A reservation as staff screens find it: in house or arriving today.
 */
public record ReservationSummary(
        String reservationId,
        String roomNumber,
        String guestName,
        LocalDate arrivalDate,
        LocalDate departureDate,
        int adults,
        int children,
        String language,
        List<Allergen> pmsAllergens,
        boolean hasPassport) {
}
