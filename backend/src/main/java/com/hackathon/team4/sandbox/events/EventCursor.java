package com.hackathon.team4.sandbox.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The {@code nextSince} position in the sandbox events feed. One row.
 */
@Entity
@Table(name = "sandbox_event_cursor")
public class EventCursor {

    static final String FEED = "sandbox";

    @Id
    @Column(name = "feed")
    private String feed;

    @Column(name = "next_since", nullable = false)
    private long nextSince;

    protected EventCursor() {
    }

    EventCursor(String feed, long nextSince) {
        this.feed = feed;
        this.nextSince = nextSince;
    }

    long nextSince() {
        return nextSince;
    }

    void moveTo(long position) {
        this.nextSince = position;
    }
}
