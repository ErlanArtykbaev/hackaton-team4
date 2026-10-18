package com.hackathon.team4.safetylog.dto;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.safetylog.LogEntryType;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One fact of the safety log; most types fill only some of the fields. {@code statement} says what happened in
 * words, e.g. the guest's consent.
 */
public record LogEntry(
        UUID id,
        OffsetDateTime at,
        LogEntryType type,
        Integer rvc,
        String outletName,
        String reservationId,
        String roomNumber,
        String memberName,
        String item,
        Allergen allergen,
        Severity severity,
        String actor,
        String statement) {
}
