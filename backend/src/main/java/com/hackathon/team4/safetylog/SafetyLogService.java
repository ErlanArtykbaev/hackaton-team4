package com.hackathon.team4.safetylog;

import com.hackathon.team4.safetylog.dto.LogEntry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The append-only safety log: what was ordered, confirmed and consented, and what changed in the passports, so the
 * hotel can prove after an incident that a dish was ordered with the guest's explicit consent.
 */
@Service
public class SafetyLogService {

    private static final Logger log = LoggerFactory.getLogger(SafetyLogService.class);

    /** The summary covers the last day: one shift view, no hotel calendar needed. */
    static final Duration SUMMARY_PERIOD = Duration.ofHours(24);

    private final SafetyLogRepository entries;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    SafetyLogService(SafetyLogRepository entries, ApplicationEventPublisher events, Clock clock) {
        this.entries = entries;
        this.events = events;
        this.clock = clock;
    }

    /** Starts an entry stamped with the current time. */
    public SafetyLogEntry.Builder entry(LogEntryType type) {
        return SafetyLogEntry.of(type, clock.instant());
    }

    @Transactional
    public SafetyLogEntry append(SafetyLogEntry entry) {
        SafetyLogEntry saved = entries.save(entry);
        log.info("Safety log {} for reservation {} by {}", saved.type(), saved.reservationId(), saved.actor());
        events.publishEvent(new LogAppended(LogEntry.from(saved)));
        return saved;
    }

    /**
     * Newest first. {@code query}: a room number (digits, exact) or a fragment of the guest's or member's name;
     * blank means everything. Every filter is optional.
     */
    @Transactional(readOnly = true)
    public List<SafetyLogEntry> search(String query, Integer rvc, LogEntryType type) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return entries.search(rvc, type, null, null);
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return entries.search(rvc, type, trimmed, null);
        }
        return entries.search(rvc, type, null, trimmed.toLowerCase(Locale.ROOT));
    }

    @Transactional(readOnly = true)
    public Counts summary() {
        Instant since = clock.instant().minus(SUMMARY_PERIOD);
        long confirmations = entries.countByAtAfterAndType(since, LogEntryType.WAITER_CONFIRMED);
        long consents = entries.countByAtAfterAndType(since, LogEntryType.GUEST_CONSENT);
        return new Counts(
                entries.countByAtAfterAndType(since, LogEntryType.ORDER_PLACED),
                // every tablet line with an allergen at the table needs exactly one confirmation or consent
                confirmations + consents,
                confirmations,
                consents,
                entries.countByAtAfterAndType(since, LogEntryType.POS_ORDER_UNCHECKED));
    }

    public record Counts(long ordersChecked, long linesWithAllergens, long waiterConfirmations, long guestConsents,
            long uncheckedPosOrders) {
    }
}
