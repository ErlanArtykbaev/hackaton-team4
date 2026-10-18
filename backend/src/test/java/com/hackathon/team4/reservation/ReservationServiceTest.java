package com.hackathon.team4.reservation;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.passport.Passport;
import com.hackathon.team4.passport.PassportRepository;
import com.hackathon.team4.reservation.dto.ReservationSummary;
import com.hackathon.team4.sandbox.generated.model.Guest;
import com.hackathon.team4.sandbox.generated.model.Hotel;
import com.hackathon.team4.sandbox.generated.model.Reservation;
import com.hackathon.team4.sandbox.generated.model.ReservationStatus;
import com.hackathon.team4.sandbox.pms.PmsGateway;
import com.hackathon.team4.sandbox.pms.ReservationQuery;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReservationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    private final PmsGateway pms = mock(PmsGateway.class);
    private final PassportRepository passports = mock(PassportRepository.class);
    private final ReservationService service = new ReservationService(pms, passports);

    @BeforeEach
    void hotelDate() {
        when(pms.hotel()).thenReturn(new Hotel().businessDate(TODAY));
        when(pms.reservations(any())).thenReturn(List.of());
        when(passports.findByReservationIdIn(anyCollection())).thenReturn(List.of());
    }

    @Test
    void shouldSearchInHouseAndTodaysArrivalsByExactRoomNumber() {
        service.search(" 518 ");

        assertThat(queries()).containsExactly(
                new ReservationQuery(ReservationStatus.IN_HOUSE, null, null, null, "518", null, null),
                new ReservationQuery(ReservationStatus.BOOKED, TODAY, TODAY, null, "518", null, null));
    }

    @Test
    void shouldSearchInHouseAndTodaysArrivalsByLastNamePrefix() {
        service.search("Ku");

        assertThat(queries()).containsExactly(
                new ReservationQuery(ReservationStatus.IN_HOUSE, null, null, null, null, "Ku", null),
                new ReservationQuery(ReservationStatus.BOOKED, TODAY, TODAY, null, null, "Ku", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "K", " k "})
    void shouldFindNothingWithoutCallingThePmsForShortQuery(String query) {
        assertThat(service.search(query)).isEmpty();

        verifyNoInteractions(pms, passports);
    }

    @Test
    void shouldMapPmsReservationToSummary() {
        when(pms.reservations(any())).thenReturn(List.of(
                reservation("R-1", 1, List.of(com.hackathon.team4.sandbox.generated.model.Allergen.PEANUTS))),
                List.of());
        when(passports.findByReservationIdIn(anyCollection())).thenReturn(List.of(passport("R-1")));

        assertThat(service.search("518")).containsExactly(new ReservationSummary("R-1", "518", "Mikhail Kuznetsov",
                LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 22), 2, 1, "ru", List.of(Allergen.PEANUTS), true));
    }

    @Test
    void shouldListOnlyGuestsWithAllergiesOrChildrenWithoutPassportFirst() {
        Reservation arrivalWithPassport = reservation("R-ARR-P", 1, List.of());
        Reservation arrivalAllergic = reservation("R-ARR",
                0, List.of(com.hackathon.team4.sandbox.generated.model.Allergen.MILK));
        Reservation arrivalPlain = reservation("R-ARR-NONE", 0, List.of());
        Reservation inHouseWithChild = reservation("R-IN", 2, List.of());
        Reservation inHousePlain = reservation("R-IN-NONE", 0, null);
        when(pms.reservations(ReservationQuery.arrivalsOn(TODAY)))
                .thenReturn(List.of(arrivalWithPassport, arrivalAllergic, arrivalPlain));
        when(pms.reservations(ReservationQuery.inHouse())).thenReturn(List.of(inHouseWithChild, inHousePlain));
        when(passports.findByReservationIdIn(anyCollection())).thenReturn(List.of(passport("R-ARR-P")));

        assertThat(service.arrivals()).extracting(ReservationSummary::reservationId)
                .containsExactly("R-ARR", "R-IN", "R-ARR-P");
    }

    @Test
    void shouldListAReservationOnceWhenTheGuestIsCheckedInBetweenTheTwoCalls() {
        Reservation guest = reservation("R-1", 1, List.of());
        when(pms.reservations(ReservationQuery.arrivalsOn(TODAY))).thenReturn(List.of(guest));
        when(pms.reservations(ReservationQuery.inHouse())).thenReturn(List.of(guest));

        assertThat(service.arrivals()).extracting(ReservationSummary::reservationId).containsExactly("R-1");
    }

    private List<ReservationQuery> queries() {
        ArgumentCaptor<ReservationQuery> captor = ArgumentCaptor.forClass(ReservationQuery.class);
        verify(pms, times(2)).reservations(captor.capture());
        return captor.getAllValues();
    }

    private static Reservation reservation(String id, int children,
                                           List<com.hackathon.team4.sandbox.generated.model.Allergen> allergies) {
        return new Reservation().id(id).roomNumber("518")
                .arrivalDate(LocalDate.of(2026, 10, 9)).departureDate(LocalDate.of(2026, 10, 22))
                .adults(2).children(children)
                .guest(new Guest().firstName("Mikhail").lastName("Kuznetsov").language("ru").allergies(allergies));
    }

    private static Passport passport(String reservationId) {
        return new Passport(reservationId, Instant.parse("2026-10-18T05:00:00Z"));
    }
}
