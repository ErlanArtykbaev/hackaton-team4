package com.hackathon.team4.safetylog;

import com.hackathon.team4.safetylog.dto.LogEntry;

/**
 * An entry was appended to the safety log. Published inside the transaction: listen with
 * {@code @TransactionalEventListener(fallbackExecution = true)}.
 */
public record LogAppended(LogEntry entry) {
}
