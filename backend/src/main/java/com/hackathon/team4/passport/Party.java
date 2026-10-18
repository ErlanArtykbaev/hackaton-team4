package com.hackathon.team4.passport;

import com.hackathon.team4.allergen.PartyMember;
import java.util.List;

/**
 * Who sits at the table for a reservation, as the allergen engine and the logs need it.
 */
public record Party(String reservationId, String roomNumber, String guestName, List<PartyMember> members) {

    public Party {
        members = List.copyOf(members);
    }
}
