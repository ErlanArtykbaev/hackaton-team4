package com.hackathon.team4.stream;

/**
 * Event names of {@code GET /api/stream}; the payload is the same record the matching REST endpoint returns.
 */
public enum StreamEvent {
    /** A {@code KitchenTicket}. */
    TICKET("ticket"),
    /** A {@code LogEntry}. */
    LOG("log"),
    /** A {@code Passport}. */
    PASSPORT("passport");

    private final String eventName;

    StreamEvent(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }
}
