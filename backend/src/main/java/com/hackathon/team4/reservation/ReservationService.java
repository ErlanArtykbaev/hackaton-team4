package com.hackathon.team4.reservation;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.reservation.dto.ReservationSummary;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Finds reservations for staff screens.
 *
 * <p>Stub (BE-A2): fixed data from the demo story; BE-A3 replaces it with filtered PMS calls.
 */
@Service
public class ReservationService {

    private static final int MIN_LAST_NAME_PREFIX = 2;

    private static final List<StubReservation> RESERVATIONS = List.of(
            new StubReservation("Kuznetsov", new ReservationSummary("R-203844", "518", "Mikhail Kuznetsov",
                    LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 24), 2, 1, "ru",
                    List.of(Allergen.PEANUTS), true)),
            new StubReservation("Thomas", new ReservationSummary("R-203981", "729", "Oliver Thomas",
                    LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 23), 2, 0, "en",
                    List.of(Allergen.GLUTEN, Allergen.FISH, Allergen.MUSTARD), false)));

    /**
     * Digits are an exact room number, anything else a last-name prefix; a prefix shorter than two characters
     * finds nothing, so a search box can call this on every keystroke.
     */
    public List<ReservationSummary> search(String query) {
        String text = query.strip();
        if (text.chars().allMatch(Character::isDigit)) {
            return RESERVATIONS.stream()
                    .map(StubReservation::summary)
                    .filter(reservation -> reservation.roomNumber().equals(text))
                    .toList();
        }
        if (text.length() < MIN_LAST_NAME_PREFIX) {
            return List.of();
        }
        String prefix = text.toLowerCase(Locale.ROOT);
        return RESERVATIONS.stream()
                .filter(reservation -> reservation.lastName().toLowerCase(Locale.ROOT).startsWith(prefix))
                .map(StubReservation::summary)
                .toList();
    }

    /**
     * Today's arrivals and in-house guests with allergies or children, those without a passport first.
     */
    public List<ReservationSummary> arrivals() {
        return RESERVATIONS.stream()
                .map(StubReservation::summary)
                .sorted(Comparator.comparing(ReservationSummary::hasPassport))
                .toList();
    }

    private record StubReservation(String lastName, ReservationSummary summary) {
    }
}
