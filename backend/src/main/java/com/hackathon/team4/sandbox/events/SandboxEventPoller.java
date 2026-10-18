package com.hackathon.team4.sandbox.events;

import com.hackathon.team4.config.SandboxProperties;
import com.hackathon.team4.sandbox.SandboxException;
import com.hackathon.team4.sandbox.generated.model.Event;
import com.hackathon.team4.sandbox.generated.model.EventListResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the sandbox events feed, drops re-delivered duplicates (same {@code key}) and publishes each new event as
 * {@link SandboxEventReceived}. The cursor and the processed keys live in the database, so a restart resumes where
 * it stopped.
 */
@Component
public class SandboxEventPoller {

    private static final Logger log = LoggerFactory.getLogger(SandboxEventPoller.class);
    private static final int HISTORY_PAGE = 500;

    private final PlatformGateway platform;
    private final EventCursorRepository cursors;
    private final ProcessedEventRepository processed;
    private final ApplicationEventPublisher publisher;
    private final SandboxProperties properties;
    private final Clock clock;

    SandboxEventPoller(PlatformGateway platform, EventCursorRepository cursors, ProcessedEventRepository processed,
                       ApplicationEventPublisher publisher, SandboxProperties properties, Clock clock) {
        this.platform = platform;
        this.cursors = cursors;
        this.processed = processed;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${sandbox.events.poll-interval}")
    void scheduledPoll() {
        if (!properties.events().enabled()) {
            return;
        }
        try {
            pollOnce();
        } catch (SandboxException failure) {
            log.warn("Events feed unavailable ({}), next attempt in {}", failure.code(),
                    properties.events().pollInterval());
        }
    }

    /**
     * Reads the feed until it is drained and returns the number of events published. Called by the scheduler;
     * tests call it directly.
     */
    public int pollOnce() {
        Optional<EventCursor> stored = cursors.findById(EventCursor.FEED);
        if (stored.isEmpty() && properties.events().skipHistory()) {
            skipHistory();
            return 0;
        }
        EventCursor cursor = stored.orElseGet(() -> new EventCursor(EventCursor.FEED, 0));
        int limit = properties.events().batchSize();
        int published = 0;
        while (true) {
            EventListResponse batch = platform.events(cursor.nextSince(), limit, null);
            List<Event> events = batch.getElements() == null ? List.of() : batch.getElements();
            for (Event event : events) {
                if (publishIfNew(event)) {
                    published++;
                }
            }
            if (batch.getNextSince() != null) {
                cursor.moveTo(batch.getNextSince());
            }
            cursors.save(cursor);
            if (events.size() < limit) {
                return published;
            }
        }
    }

    /** Moves a new cursor to the end of the feed without publishing anything: the feed outlives hotel resets. */
    private void skipHistory() {
        EventCursor cursor = new EventCursor(EventCursor.FEED, 0);
        int limit = HISTORY_PAGE;
        int skipped = 0;
        while (true) {
            EventListResponse batch = platform.events(cursor.nextSince(), limit, null);
            int size = batch.getElements() == null ? 0 : batch.getElements().size();
            skipped += size;
            if (batch.getNextSince() != null) {
                cursor.moveTo(batch.getNextSince());
            }
            if (size < limit) {
                break;
            }
        }
        cursors.save(cursor);
        log.info("Skipped {} events of the feed history; live from position {}", skipped, cursor.nextSince());
    }

    private boolean publishIfNew(Event event) {
        if (event.getKey() == null || processed.existsById(event.getKey())) {
            return false;
        }
        try {
            publisher.publishEvent(new SandboxEventReceived(event));
        } catch (RuntimeException listenerFailure) {
            // One broken listener must not stop the feed for everyone else.
            log.error("Listener failed for sandbox event {}", event.getKey(), listenerFailure);
        }
        processed.save(new ProcessedEvent(event.getKey(), Instant.now(clock)));
        return true;
    }
}
