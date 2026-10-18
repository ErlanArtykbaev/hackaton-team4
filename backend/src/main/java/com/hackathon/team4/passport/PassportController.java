package com.hackathon.team4.passport;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.menu.OutletService;
import com.hackathon.team4.passport.dto.MemberAllergyRequest;
import com.hackathon.team4.passport.dto.PassportMemberRequest;
import com.hackathon.team4.passport.dto.PassportResponse;
import com.hackathon.team4.passport.dto.PassportSource;
import com.hackathon.team4.passport.dto.PassportUpdateRequest;
import jakarta.validation.Valid;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final OutletService outlets;

    public PassportController(PassportService passports, OutletService outlets) {
        this.passports = passports;
        this.outlets = outlets;
    }

    /** Without a saved passport: the primary guest with the PMS allergens as severe. */
    @GetMapping("/{reservationId}")
    public PassportResponse passport(@PathVariable String reservationId) {
        return PassportResponse.from(passports.get(reservationId));
    }

    /** Saves all cards; the PMS profile gets the union of the members' allergens. */
    @PutMapping("/{reservationId}")
    public PassportResponse update(@PathVariable String reservationId,
                                   @Valid @RequestBody PassportUpdateRequest update) {
        String outletName = update.rvc() == null ? null : outlets.outletName(update.rvc()).orElse(null);
        List<PassportSnapshot.Member> members = update.members().stream()
                .map(PassportController::toMember)
                .toList();
        return PassportResponse.from(passports.save(reservationId, update.notes(), members, update.updatedBy(),
                toSource(update.source()), update.rvc(), outletName));
    }

    private static PassportSnapshot.Member toMember(PassportMemberRequest member) {
        Map<Allergen, Severity> allergies = member.allergies().stream()
                .collect(Collectors.toMap(MemberAllergyRequest::allergen, MemberAllergyRequest::severity,
                        (first, second) -> second, () -> new EnumMap<>(Allergen.class)));
        return new PassportSnapshot.Member(member.id(), member.name(), member.age(), member.primary(), allergies);
    }

    private static PassportService.Source toSource(PassportSource source) {
        return switch (source) {
            case FRONT_DESK -> PassportService.Source.FRONT_DESK;
            case WAITER -> PassportService.Source.WAITER;
        };
    }
}
