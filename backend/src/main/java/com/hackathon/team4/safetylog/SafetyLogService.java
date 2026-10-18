package com.hackathon.team4.safetylog;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.safetylog.dto.LogEntry;
import com.hackathon.team4.safetylog.dto.SafetyLogSummary;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The safety log: proof of what was ordered, confirmed and consented to, searchable by room or last name.
 *
 * <p>Stub (BE-A2): fixed entries from the demo story for room 518, one or more per type; BE-B4 replaces them with
 * the stored log.
 */
@Service
public class SafetyLogService {

    private static final int MIN_LAST_NAME_PREFIX = 2;
    private static final String POOL_BAR = "Pool Bar";

    private static final List<StubEntry> ENTRIES = List.of(
            entry(6, at(18, 50), LogEntryType.POS_ORDER_UNCHECKED, 4, POOL_BAR, "Misha", "Som tam",
                    Allergen.PEANUTS, Severity.SEVERE, "B01",
                    "Typed straight into the POS, not checked at ordering"),
            entry(5, at(18, 45), LogEntryType.GUEST_CONSENT, 4, POOL_BAR, "Anna", "Thai iced tea",
                    Allergen.MILK, Severity.INTOLERANCE, "W02",
                    "Anna Kuznetsova: I know it contains MILK and I order it"),
            entry(4, at(18, 45), LogEntryType.WAITER_CONFIRMED, 4, POOL_BAR, "Misha", "Chicken satay",
                    Allergen.PEANUTS, Severity.SEVERE, "W02",
                    "Ordered for Mikhail; Misha (severe) is at the table"),
            entry(3, at(18, 45), LogEntryType.ORDER_PLACED, 4, POOL_BAR, null, null,
                    null, null, "W02",
                    "Check 1017 sent: 5 lines, 2 with allergens"),
            entry(2, at(18, 42), LogEntryType.PASSPORT_UPDATED, 4, POOL_BAR, "Anna", null,
                    Allergen.MILK, Severity.INTOLERANCE, "W02",
                    "Added MILK (intolerance) for Anna after asking the guest"),
            entry(1, at(15, 10), LogEntryType.PASSPORT_UPDATED, null, null, "Misha", null,
                    Allergen.PEANUTS, Severity.SEVERE, "Front desk",
                    "Passport saved at check-in: Mikhail, Anna, Misha (7)"));

    private static final SafetyLogSummary SUMMARY = new SafetyLogSummary(1, 2, 1, 1, 1);

    /** All filters are optional; {@code query} is a room number (exact) or a last-name prefix. Newest first. */
    public List<LogEntry> search(String query, Integer rvc, LogEntryType type) {
        return ENTRIES.stream()
                .filter(stub -> matches(stub, query))
                .map(StubEntry::entry)
                .filter(entry -> rvc == null || rvc.equals(entry.rvc()))
                .filter(entry -> type == null || type == entry.type())
                .toList();
    }

    public SafetyLogSummary summary() {
        return SUMMARY;
    }

    private static boolean matches(StubEntry stub, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String text = query.strip();
        if (text.chars().allMatch(Character::isDigit)) {
            return text.equals(stub.entry().roomNumber());
        }
        return text.length() >= MIN_LAST_NAME_PREFIX
                && stub.lastName().toLowerCase(Locale.ROOT).startsWith(text.toLowerCase(Locale.ROOT));
    }

    private static StubEntry entry(int number, OffsetDateTime at, LogEntryType type, Integer rvc, String outletName,
                                   String memberName, String item, Allergen allergen, Severity severity, String actor,
                                   String statement) {
        UUID id = UUID.fromString("10600000-0000-4000-8000-00000000000" + number);
        return new StubEntry("Kuznetsov", new LogEntry(id, at, type, rvc, outletName, "R-203844", "518", memberName,
                item, allergen, severity, actor, statement));
    }

    private static OffsetDateTime at(int hour, int minute) {
        return OffsetDateTime.of(2026, 10, 18, hour, minute, 0, 0, ZoneOffset.ofHours(7));
    }

    private record StubEntry(String lastName, LogEntry entry) {
    }
}
