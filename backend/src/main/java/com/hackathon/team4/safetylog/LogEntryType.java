package com.hackathon.team4.safetylog;

public enum LogEntryType {
    /** A passport card was created or changed (front desk, or the waiter after asking the guest). */
    PASSPORT_UPDATED,
    /** An order was sent to the POS through our tablet. */
    ORDER_PLACED,
    /** The waiter confirmed a line for a safe member while another member at the table is allergic. */
    WAITER_CONFIRMED,
    /** The guest confirmed by name that they order a dish containing their allergen. */
    GUEST_CONSENT,
    /** A round typed straight into the POS for a known reservation, with allergens at the table. */
    POS_ORDER_UNCHECKED
}
