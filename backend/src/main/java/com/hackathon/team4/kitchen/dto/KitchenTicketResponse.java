package com.hackathon.team4.kitchen.dto;

import com.hackathon.team4.kitchen.KitchenTicket;
import com.hackathon.team4.kitchen.TicketSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    public static KitchenTicketResponse from(KitchenTicket ticket) {
        List<KitchenTicketLine> lines = ticket.lines().stream().map(KitchenTicketLine::from).toList();
        return new KitchenTicketResponse(ticket.id(), ticket.rvc(), Integer.parseInt(ticket.checkNumber()),
                ticket.table(), ticket.roomNumber(), ticket.guestName(), ticket.sentAt().atOffset(ZoneOffset.UTC),
                ticket.source(), ticket.hasAllergens(), lines);
    }
}
