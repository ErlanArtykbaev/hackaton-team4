package com.hackathon.team4.safetylog.dto;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogEntry;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    public static LogEntry from(SafetyLogEntry entry) {
        return new LogEntry(entry.id(), entry.at().atOffset(ZoneOffset.UTC), entry.type(), entry.rvc(),
                entry.outletName(), entry.reservationId(), entry.roomNumber(), entry.memberName(), entry.item(),
                entry.allergen(), entry.severity(), entry.actor(), entry.statement());
    }
}
