package com.hackathon.team4.passport;

import com.hackathon.team4.passport.dto.PassportResponse;
import com.hackathon.team4.passport.dto.PassportUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The allergy passport of a reservation, for the front desk and the waiter.
 */
@RestController
@RequestMapping("/api/passports")
public class PassportController {

    private final PassportService passports;

    public PassportController(PassportService passports) {
        this.passports = passports;
    }

    /** Without a saved passport: the primary guest with the PMS allergens as severe. */
    @GetMapping("/{reservationId}")
    public PassportResponse passport(@PathVariable String reservationId) {
        return passports.passport(reservationId);
    }

    /** Saves all cards; the PMS profile gets the union of the members' allergens. */
    @PutMapping("/{reservationId}")
    public PassportResponse update(@PathVariable String reservationId,
                                   @Valid @RequestBody PassportUpdateRequest update) {
        return passports.update(reservationId, update);
    }
}
