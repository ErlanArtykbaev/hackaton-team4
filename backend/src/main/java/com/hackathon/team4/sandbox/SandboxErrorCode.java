package com.hackathon.team4.sandbox;

import java.util.Arrays;
import java.util.Set;

/**
 * Error codes of the sandbox contract, plus {@link #NETWORK_ERROR} and {@link #UNKNOWN} for failures that carry no code.
 */
public enum SandboxErrorCode {
    VALIDATION_ERROR,
    UNAUTHORIZED,
    AUTH_INVALID,
    NOT_FOUND,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    PMS_UNAVAILABLE,
    ROOM_NOT_READY,
    ROOM_OCCUPIED,
    INVALID_STATUS_TRANSITION,
    GUEST_NOT_IN_HOUSE,
    POSTING_BLOCKED,
    CREDIT_LIMIT_EXCEEDED,
    OCCUPIED_TABLE,
    UNKNOWN_ORDER_TYPE,
    UNKNOWN_MENU_ITEM,
    UNKNOWN_TENDER,
    CHECK_CLOSED,
    PAYMENT_EXCEEDS_DUE,
    /** No HTTP response: connection refused, timeout. */
    NETWORK_ERROR,
    /** A response without a recognisable code. */
    UNKNOWN;

    private static final Set<SandboxErrorCode> TRANSIENT =
            Set.of(RATE_LIMITED, SERVICE_UNAVAILABLE, PMS_UNAVAILABLE, NETWORK_ERROR);

    private static final Set<SandboxErrorCode> BUSINESS = Set.of(
            ROOM_NOT_READY, ROOM_OCCUPIED, INVALID_STATUS_TRANSITION, GUEST_NOT_IN_HOUSE, POSTING_BLOCKED,
            CREDIT_LIMIT_EXCEEDED, OCCUPIED_TABLE, CHECK_CLOSED, PAYMENT_EXCEEDS_DUE);

    /** The sandbox may succeed if the same call is repeated later. */
    public boolean isTransient() {
        return TRANSIENT.contains(this);
    }

    /** A hotel rule refused the action; the user can act on it. */
    public boolean isBusinessRule() {
        return BUSINESS.contains(this);
    }

    public static SandboxErrorCode fromContract(String code) {
        return Arrays.stream(values())
                .filter(value -> value.name().equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
