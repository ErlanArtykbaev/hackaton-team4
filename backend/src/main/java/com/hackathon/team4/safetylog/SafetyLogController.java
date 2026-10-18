package com.hackathon.team4.safetylog;

import com.hackathon.team4.safetylog.dto.LogEntry;
import com.hackathon.team4.safetylog.dto.SafetyLogSummary;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The safety log for the shift manager.
 */
@RestController
@RequestMapping("/api/safety-log")
public class SafetyLogController {

    private final SafetyLogService log;

    public SafetyLogController(SafetyLogService log) {
        this.log = log;
    }

    /**
     * {@code query} is a room number (exact) or a fragment of the guest's or member's name; every filter is optional.
     * Newest first.
     */
    @GetMapping
    public List<LogEntry> search(@RequestParam(required = false) String query,
                                 @RequestParam(required = false) Integer rvc,
                                 @RequestParam(required = false) LogEntryType type) {
        return log.search(query, rvc, type).stream().map(SafetyLogController::toResponse).toList();
    }

    /** The last day. */
    @GetMapping("/summary")
    public SafetyLogSummary summary() {
        SafetyLogService.Counts counts = log.summary();
        return new SafetyLogSummary(counts.ordersChecked(), counts.linesWithAllergens(), counts.waiterConfirmations(),
                counts.guestConsents(), counts.uncheckedPosOrders());
    }

    private static LogEntry toResponse(SafetyLogEntry entry) {
        return new LogEntry(entry.id(), entry.at().atOffset(ZoneOffset.UTC), entry.type(), entry.rvc(),
                entry.outletName(), entry.reservationId(), entry.roomNumber(), entry.memberName(), entry.item(),
                entry.allergen(), entry.severity(), entry.actor(), entry.statement());
    }
}
