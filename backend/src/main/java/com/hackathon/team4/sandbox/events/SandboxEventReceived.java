package com.hackathon.team4.sandbox.events;

import com.hackathon.team4.sandbox.generated.model.Event;

/**
 * Published once per business event of the sandbox feed (duplicates already removed). Features listen with
 * {@code @EventListener} and filter by {@code event.getType()}.
 */
public record SandboxEventReceived(Event event) {
}
