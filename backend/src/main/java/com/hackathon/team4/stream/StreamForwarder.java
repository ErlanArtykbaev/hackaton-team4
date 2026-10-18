package com.hackathon.team4.stream;

import com.hackathon.team4.kitchen.TicketChanged;
import com.hackathon.team4.passport.PassportChanged;
import com.hackathon.team4.safetylog.LogAppended;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends BE-B's domain events to the staff screens once their transaction has committed, so a screen never shows
 * data that was rolled back. Outside a transaction the event goes out at once.
 */
@Component
class StreamForwarder {

    private final StreamHub hub;

    StreamForwarder(StreamHub hub) {
        this.hub = hub;
    }

    @TransactionalEventListener(fallbackExecution = true)
    void onPassportChanged(PassportChanged changed) {
        hub.broadcast(StreamEvent.PASSPORT, changed.passport());
    }

    @TransactionalEventListener(fallbackExecution = true)
    void onTicketChanged(TicketChanged changed) {
        hub.broadcast(StreamEvent.TICKET, changed.ticket());
    }

    @TransactionalEventListener(fallbackExecution = true)
    void onLogAppended(LogAppended appended) {
        hub.broadcast(StreamEvent.LOG, appended.entry());
    }
}
