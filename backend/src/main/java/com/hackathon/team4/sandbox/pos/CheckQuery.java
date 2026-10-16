package com.hackathon.team4.sandbox.pos;

import com.hackathon.team4.sandbox.generated.model.CheckStatus;
import java.time.LocalDate;

/**
 * Filters of {@code GET /v1/pos/checks}; {@code null} means "any" ({@code status} defaults to OPEN in the sandbox).
 */
public record CheckQuery(Integer rvc, CheckStatus status, LocalDate from, LocalDate to, String reservationId) {

    public static CheckQuery open() {
        return new CheckQuery(null, CheckStatus.OPEN, null, null, null);
    }

    public static CheckQuery openIn(int rvc) {
        return new CheckQuery(rvc, CheckStatus.OPEN, null, null, null);
    }

    public static CheckQuery ofReservation(String reservationId, CheckStatus status) {
        return new CheckQuery(null, status, null, null, reservationId);
    }
}
