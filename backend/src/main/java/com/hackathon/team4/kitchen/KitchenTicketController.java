package com.hackathon.team4.kitchen;

import com.hackathon.team4.kitchen.dto.KitchenTicketResponse;
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
        return tickets.tickets(rvc).stream().map(KitchenTicketResponse::from).toList();
    }
}
