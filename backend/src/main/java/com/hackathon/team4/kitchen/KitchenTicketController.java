package com.hackathon.team4.kitchen;

import com.hackathon.team4.kitchen.dto.KitchenTicketLine;
import com.hackathon.team4.kitchen.dto.KitchenTicketResponse;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The kitchen display of an outlet: every round sent to the kitchen with its allergens and for whom.
 */
@RestController
public class KitchenTicketController {

    private final KitchenTicketService tickets;

    public KitchenTicketController(KitchenTicketService tickets) {
        this.tickets = tickets;
    }

    /** The current shift, newest first. */
    @GetMapping("/api/kitchen/{rvc}/tickets")
    public List<KitchenTicketResponse> tickets(@PathVariable int rvc) {
        return tickets.tickets(rvc).stream().map(KitchenTicketController::toResponse).toList();
    }

    private static KitchenTicketResponse toResponse(KitchenTicket ticket) {
        List<KitchenTicketLine> lines = ticket.lines().stream().map(KitchenTicketController::toLine).toList();
        return new KitchenTicketResponse(ticket.id(), ticket.rvc(), Integer.parseInt(ticket.checkNumber()),
                ticket.table(), ticket.roomNumber(), ticket.guestName(), ticket.sentAt().atOffset(ZoneOffset.UTC),
                ticket.source(), ticket.hasAllergens(), lines);
    }

    private static KitchenTicketLine toLine(TicketLine line) {
        return new KitchenTicketLine(line.name(), line.quantity(), line.condiments(), line.allergens(),
                line.verdict(), line.dangerFor(), line.forMemberName(), line.waiterConfirmed(), line.consentBy());
    }
}
