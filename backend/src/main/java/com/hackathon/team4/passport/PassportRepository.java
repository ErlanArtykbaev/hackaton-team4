package com.hackathon.team4.passport;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportRepository extends JpaRepository<Passport, UUID> {

    @EntityGraph(attributePaths = {"members", "members.allergies"})
    Optional<Passport> findByReservationId(String reservationId);

    /** Passports of many reservations at once, for the kitchen and the menu verdicts. */
    @EntityGraph(attributePaths = {"members", "members.allergies"})
    List<Passport> findByReservationIdIn(Collection<String> reservationIds);

    @EntityGraph(attributePaths = {"members", "members.allergies"})
    List<Passport> findByGuestId(String guestId);

    boolean existsByReservationId(String reservationId);
}
