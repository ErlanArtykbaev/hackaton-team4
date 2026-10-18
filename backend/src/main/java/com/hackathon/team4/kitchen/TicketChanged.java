package com.hackathon.team4.kitchen;

import com.hackathon.team4.kitchen.dto.KitchenTicketResponse;

/**
 * A kitchen ticket was created or replaced (a tablet round replacing the feed's ticket for the same round).
 * Published inside the transaction: listen with {@code @TransactionalEventListener(fallbackExecution = true)}.
 */
public record TicketChanged(KitchenTicketResponse ticket) {
}
