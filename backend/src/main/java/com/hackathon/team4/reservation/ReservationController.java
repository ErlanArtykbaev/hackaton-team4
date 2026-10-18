package com.hackathon.team4.reservation;

import com.hackathon.team4.reservation.dto.ReservationSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reservation search for the waiter and the front desk, and the front-desk worklist.
 */
@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    /** Room number (exact) or last-name prefix (2+ characters); in-house guests and today's arrivals. */
    @GetMapping("/reservations")
    public List<ReservationSummary> search(@RequestParam(defaultValue = "") String query) {
        return reservations.search(query);
    }

    @GetMapping("/front-desk/arrivals")
    public List<ReservationSummary> arrivals() {
        return reservations.arrivals();
    }
}
