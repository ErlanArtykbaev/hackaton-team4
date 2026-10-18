package com.hackathon.team4.reservation;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.passport.Passport;
import com.hackathon.team4.passport.PassportRepository;
import com.hackathon.team4.reservation.dto.ReservationSummary;
import com.hackathon.team4.sandbox.generated.model.Guest;
import com.hackathon.team4.sandbox.generated.model.Reservation;
import com.hackathon.team4.sandbox.generated.model.ReservationStatus;
import com.hackathon.team4.sandbox.pms.PmsGateway;
import com.hackathon.team4.sandbox.pms.ReservationQuery;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Finds reservations for staff screens: in-house guests and today's arrivals, read from the PMS. Every reservation
 * list call is filtered by status (an unfiltered list takes 8–11 s); in-house reservations whose departure date has
 * passed still count as in house.
 */
@Service
public class ReservationService {

    private static final int MIN_LAST_NAME_PREFIX = 2;

    private final PmsGateway pms;
    private final PassportRepository passports;

    public ReservationService(PmsGateway pms, PassportRepository passports) {
        this.pms = pms;
        this.passports = passports;
    }

    /**
     * Digits are an exact room number, anything else a last-name prefix; a prefix shorter than two characters
     * finds nothing without calling the PMS, so a search box can call this on every keystroke.
     */
    public List<ReservationSummary> search(String query) {
        String text = query.strip();
        if (text.length() < MIN_LAST_NAME_PREFIX && !isRoomNumber(text)) {
            return List.of();
        }
        String roomNumber = isRoomNumber(text) ? text : null;
        String lastName = roomNumber == null ? text : null;
        LocalDate today = pms.hotel().getBusinessDate();
        return summaries(Stream.concat(
                pms.reservations(new ReservationQuery(
                        ReservationStatus.IN_HOUSE, null, null, null, roomNumber, lastName, null)).stream(),
                pms.reservations(new ReservationQuery(
                        ReservationStatus.BOOKED, today, today, null, roomNumber, lastName, null)).stream()));
    }

    /**
     * Today's arrivals and in-house guests with allergies on the PMS profile or children, those without a passport
     * first; arrivals before in-house guests otherwise.
     */
    public List<ReservationSummary> arrivals() {
        LocalDate today = pms.hotel().getBusinessDate();
        Stream<Reservation> candidates = Stream.concat(
                pms.reservations(ReservationQuery.arrivalsOn(today)).stream(),
                pms.reservations(ReservationQuery.inHouse()).stream());
        return summaries(candidates.filter(ReservationService::needsPassport)).stream()
                .sorted(Comparator.comparing(ReservationSummary::hasPassport))
                .toList();
    }

    private static boolean isRoomNumber(String text) {
        return !text.isEmpty() && text.chars().allMatch(Character::isDigit);
    }

    private static boolean needsPassport(Reservation reservation) {
        return !pmsAllergens(reservation).isEmpty() || count(reservation.getChildren()) > 0;
    }

    /** Maps in order, once per reservation: the simulator may check a guest in between the two list calls. */
    private List<ReservationSummary> summaries(Stream<Reservation> reservations) {
        Map<String, Reservation> byId = new LinkedHashMap<>();
        reservations.forEach(reservation -> byId.putIfAbsent(reservation.getId(), reservation));
        if (byId.isEmpty()) {
            return List.of();
        }
        Set<String> withPassport = passports.findByReservationIdIn(byId.keySet()).stream()
                .map(Passport::reservationId)
                .collect(Collectors.toSet());
        return byId.values().stream()
                .map(reservation -> summary(reservation, withPassport.contains(reservation.getId())))
                .toList();
    }

    private static ReservationSummary summary(Reservation reservation, boolean hasPassport) {
        Guest guest = reservation.getGuest();
        return new ReservationSummary(
                reservation.getId(),
                reservation.getRoomNumber(),
                guestName(guest),
                reservation.getArrivalDate(),
                reservation.getDepartureDate(),
                count(reservation.getAdults()),
                count(reservation.getChildren()),
                guest == null ? null : guest.getLanguage(),
                pmsAllergens(reservation),
                hasPassport);
    }

    private static List<Allergen> pmsAllergens(Reservation reservation) {
        Guest guest = reservation.getGuest();
        if (guest == null || guest.getAllergies() == null) {
            return List.of();
        }
        return guest.getAllergies().stream()
                .filter(Objects::nonNull)
                .map(allergen -> Allergen.valueOf(allergen.name()))
                .toList();
    }

    private static String guestName(Guest guest) {
        if (guest == null) {
            return "";
        }
        return String.join(" ", Objects.toString(guest.getFirstName(), ""), Objects.toString(guest.getLastName(), ""))
                .trim();
    }

    private static int count(Integer value) {
        return value == null ? 0 : value;
    }
}
