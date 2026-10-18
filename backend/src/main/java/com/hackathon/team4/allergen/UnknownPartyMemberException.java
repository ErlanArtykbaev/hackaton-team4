package com.hackathon.team4.allergen;

import java.util.UUID;

/**
 * An order line names a "for whom" member that is not in the party.
 */
public class UnknownPartyMemberException extends RuntimeException {

    public UnknownPartyMemberException(UUID memberId) {
        super("Member " + memberId + " is not in the party");
    }
}
