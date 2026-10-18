package com.hackathon.team4.kitchen;

public enum TicketSource {
    /** Ordered through our tablet: checked at ordering. */
    CHECKED,
    /** Typed straight into the POS for a known reservation: not checked at ordering. */
    UNCHECKED,
    /** No reservation on the check: allergens only, guest unknown. */
    UNLINKED
}
