package com.hackathon.team4.stream;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The open server-sent-event connections of the staff screens. Every screen gets every event and picks what it
 * needs; a connection that fails on send is dropped, the browser reconnects and refetches.
 */
@Component
public class StreamHub {

    private static final Logger log = LoggerFactory.getLogger(StreamHub.class);

    /** No server-side timeout: the connection lives as long as the screen. */
    private static final long NO_TIMEOUT = 0L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter connect() {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(failure -> emitters.remove(emitter));
        emitters.add(emitter);
        // a first write flushes the headers, so the browser reports the stream as open at once
        send(emitter, SseEmitter.event().comment("connected"));
        log.debug("Stream connected, {} open", emitters.size());
        return emitter;
    }

    /** Sends one event to every open screen. */
    public void broadcast(StreamEvent event, Object payload) {
        SseEmitter.SseEventBuilder message = SseEmitter.event().name(event.eventName()).data(payload);
        emitters.forEach(emitter -> send(emitter, message));
    }

    /** Keeps idle connections open through proxies and finds the ones whose screen is gone. */
    @Scheduled(fixedRate = 15_000)
    void heartbeat() {
        emitters.forEach(emitter -> send(emitter, SseEmitter.event().comment("heartbeat")));
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder message) {
        try {
            emitter.send(message);
        } catch (IOException | IllegalStateException gone) {
            emitters.remove(emitter);
            emitter.completeWithError(gone);
            log.debug("Stream closed by the client, {} open", emitters.size());
        }
    }
}
