package com.hackathon.team4.allergen;

/**
 * What an order line needs before it is sent to the POS. Nothing is ever blocked.
 */
public enum Requirement {
    /** Nobody at the table is allergic to the line. */
    NONE,
    /** Another member at the table is allergic; the waiter confirms with one tap. */
    WAITER_CONFIRMATION,
    /** The line is for an allergic member, or for the table while a member is allergic; the guest consents. */
    GUEST_CONSENT
}
