package com.hackathon.team4.passport;

import com.hackathon.team4.passport.dto.PassportResponse;

/**
 * A passport was saved (front desk, waiter, or an allergen set directly in the PMS). Published inside the
 * transaction: listen with {@code @TransactionalEventListener(fallbackExecution = true)} so screens only see
 * committed data.
 */
public record PassportChanged(PassportResponse passport) {
}
