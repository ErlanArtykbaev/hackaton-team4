package com.hackathon.team4.passport;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A reservation's passport as staff screens see it: our member cards next to what the PMS profile holds.
 * {@code updatedAt} is {@code null} until the first save.
 */
public record PassportSnapshot(String reservationId, String roomNumber, String guestName, String notes,
        List<Allergen> pmsAllergens, List<Member> members, Instant updatedAt, String updatedBy) {

    public PassportSnapshot {
        pmsAllergens = List.copyOf(pmsAllergens);
        members = List.copyOf(members);
    }

    public record Member(UUID id, String name, Integer age, boolean primary, Map<Allergen, Severity> allergies) {

        public Member {
            allergies = Map.copyOf(allergies);
        }
    }
}
