package com.hackathon.team4.sandbox.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The business key of an event already published, so a re-delivered duplicate is skipped.
 */
@Entity
@Table(name = "sandbox_processed_event")
public class ProcessedEvent {

    @Id
    @Column(name = "event_key")
    private String eventKey;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    ProcessedEvent(String eventKey, Instant processedAt) {
        this.eventKey = eventKey;
        this.processedAt = processedAt;
    }
}
