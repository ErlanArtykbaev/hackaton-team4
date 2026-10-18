package com.hackathon.team4.safetylog;

import com.hackathon.team4.safetylog.dto.LogEntry;
import com.hackathon.team4.safetylog.dto.SafetyLogSummary;
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

    /** {@code query} is a room number (exact) or a last-name prefix; every filter is optional. Newest first. */
    @GetMapping
    public List<LogEntry> search(@RequestParam(required = false) String query,
                                 @RequestParam(required = false) Integer rvc,
                                 @RequestParam(required = false) LogEntryType type) {
        return log.search(query, rvc, type);
    }

    @GetMapping("/summary")
    public SafetyLogSummary summary() {
        return log.summary();
    }
}
