package com.hackathon.team4.sandbox.pms;

import com.hackathon.team4.sandbox.generated.model.ReservationStatus;
import java.time.LocalDate;

/**
 * Filters of {@code GET /v1/pms/reservations}; {@code null} means "any".
 */
public record ReservationQuery(
        ReservationStatus status,
        LocalDate arrivalFrom,
        LocalDate arrivalTo,
        LocalDate departureDate,
        String roomNumber,
        String lastName,
        String groupCode) {

    public static ReservationQuery any() {
        return new ReservationQuery(null, null, null, null, null, null, null);
    }

    public static ReservationQuery inHouse() {
        return any().withStatus(ReservationStatus.IN_HOUSE);
    }

    public static ReservationQuery arrivalsOn(LocalDate businessDate) {
        return new ReservationQuery(ReservationStatus.BOOKED, businessDate, businessDate, null, null, null, null);
    }

    public static ReservationQuery departuresOn(LocalDate businessDate) {
        return new ReservationQuery(ReservationStatus.IN_HOUSE, null, null, businessDate, null, null, null);
    }

    public static ReservationQuery byLastName(String lastNamePrefix) {
        return new ReservationQuery(null, null, null, null, null, lastNamePrefix, null);
    }

    public ReservationQuery withStatus(ReservationStatus newStatus) {
        return new ReservationQuery(newStatus, arrivalFrom, arrivalTo, departureDate, roomNumber, lastName, groupCode);
    }
}
