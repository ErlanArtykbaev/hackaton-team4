package com.hackathon.team4.safetylog.dto;

/**
 * Key figures of the safety log for the manager.
 */
public record SafetyLogSummary(
        long ordersChecked,
        long linesWithAllergens,
        long waiterConfirmations,
        long guestConsents,
        long uncheckedPosOrders) {
}
