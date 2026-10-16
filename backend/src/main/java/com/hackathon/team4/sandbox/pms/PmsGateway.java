package com.hackathon.team4.sandbox.pms;

import com.hackathon.team4.sandbox.Pages;
import com.hackathon.team4.sandbox.SandboxCalls;
import com.hackathon.team4.sandbox.generated.api.PmsApi;
import com.hackathon.team4.sandbox.generated.model.Folio;
import com.hackathon.team4.sandbox.generated.model.FolioPostingRequest;
import com.hackathon.team4.sandbox.generated.model.ForecastDay;
import com.hackathon.team4.sandbox.generated.model.Guest;
import com.hackathon.team4.sandbox.generated.model.GuestPatch;
import com.hackathon.team4.sandbox.generated.model.Hotel;
import com.hackathon.team4.sandbox.generated.model.Reservation;
import com.hackathon.team4.sandbox.generated.model.ReservationListResponse;
import com.hackathon.team4.sandbox.generated.model.ReservationPatch;
import com.hackathon.team4.sandbox.generated.model.Room;
import com.hackathon.team4.sandbox.generated.model.RoomListResponse;
import com.hackathon.team4.sandbox.generated.model.RoomPatch;
import com.hackathon.team4.sandbox.generated.model.RoomStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

import static com.hackathon.team4.sandbox.Pages.MAX_SIZE;

/**
 * The mini-PMS: hotel, rooms, reservations, guests, folios, forecast. Envelopes are unwrapped, lists fully paged.
 */
@Component
public class PmsGateway {

    private final PmsApi api;
    private final SandboxCalls calls;

    PmsGateway(PmsApi api, SandboxCalls calls) {
        this.api = api;
        this.calls = calls;
    }

    public Hotel hotel() {
        return calls.safe(() -> api.getHotel().getHotel());
    }

    /** All rooms, optionally filtered by status and floor ({@code null} = any). */
    public List<Room> rooms(RoomStatus status, Integer floor) {
        return Pages.all(
                page -> calls.safe(() -> api.listRooms(status, floor, page, MAX_SIZE)),
                RoomListResponse::getElements,
                RoomListResponse::getPageMetadata);
    }

    public Room updateRoom(String number, RoomPatch patch) {
        return calls.safe(() -> api.updateRoom(number, patch).getRoom());
    }

    public List<Reservation> reservations(ReservationQuery q) {
        return Pages.all(
                page -> calls.safe(() -> api.listReservations(q.status(), q.arrivalFrom(), q.arrivalTo(),
                        q.departureDate(), q.roomNumber(), q.lastName(), q.groupCode(), page, MAX_SIZE)),
                ReservationListResponse::getElements,
                ReservationListResponse::getPageMetadata);
    }

    public Reservation reservation(String reservationId) {
        return calls.safe(() -> api.getReservation(reservationId).getReservation());
    }

    /** Check-in, check-out, room move, late checkout. */
    public Reservation updateReservation(String reservationId, ReservationPatch patch) {
        return calls.safe(() -> api.updateReservation(reservationId, patch).getReservation());
    }

    /** Allergies, preferences, consent. Sent arrays replace the stored ones as a whole. */
    public Guest updateGuest(String guestId, GuestPatch patch) {
        return calls.safe(() -> api.updateGuest(guestId, patch).getGuest());
    }

    public Folio folio(String reservationId) {
        return calls.safe(() -> api.getFolio(reservationId).getFolio());
    }

    /**
     * Posts a charge (positive) or an adjustment (negative). The caller generates {@code postingGuid} once per
     * business action and stores it before calling, so a retry never posts twice.
     */
    public Folio postToFolio(String reservationId, FolioPostingRequest posting) {
        Objects.requireNonNull(posting.getPostingGuid(), "postingGuid is required for a folio posting");
        return calls.safe(() -> api.postToFolio(reservationId, posting).getFolio());
    }

    /** At most 62 days. */
    public List<ForecastDay> forecast(LocalDate from, LocalDate to) {
        return calls.safe(() -> api.getForecast(from, to).getElements());
    }
}
