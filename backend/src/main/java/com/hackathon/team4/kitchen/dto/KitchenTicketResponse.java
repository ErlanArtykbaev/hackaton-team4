package com.hackathon.team4.kitchen.dto;

import com.hackathon.team4.kitchen.TicketSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One round sent to the kitchen. The kitchen is notified only: a ticket is never acknowledged or approved.
 * {@code roomNumber} and {@code guestName} are {@code null} for a check without a reservation.
 */
@Schema(name = "KitchenTicket")
public record KitchenTicketResponse(
        UUID id,
        int rvc,
        int checkNumber,
        String table,
        String roomNumber,
        String guestName,
        OffsetDateTime sentAt,
        TicketSource source,
        boolean hasAllergens,
        List<KitchenTicketLine> lines) {
}
