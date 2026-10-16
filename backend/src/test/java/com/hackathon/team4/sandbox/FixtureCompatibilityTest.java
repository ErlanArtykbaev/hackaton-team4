package com.hackathon.team4.sandbox;

import com.hackathon.team4.sandbox.generated.model.CheckDTO;
import com.hackathon.team4.sandbox.generated.model.EventListResponse;
import com.hackathon.team4.sandbox.generated.model.MenuItemDTO;
import com.hackathon.team4.sandbox.generated.model.Reservation;
import com.hackathon.team4.sandbox.generated.model.Room;
import com.hackathon.team4.sandbox.events.PlatformGateway;
import com.hackathon.team4.sandbox.pms.PmsGateway;
import com.hackathon.team4.sandbox.pms.ReservationQuery;
import com.hackathon.team4.sandbox.pos.CheckQuery;
import com.hackathon.team4.sandbox.pos.PosGateway;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The generated client reads every response recorded from the practice hotel through our gateways.
 */
class FixtureCompatibilityTest extends SandboxTestBase {

    @Autowired
    private PmsGateway pms;

    @Autowired
    private PosGateway pos;

    @Autowired
    private PlatformGateway platform;

    @Test
    void shouldReadPmsFixtures() {
        stubs.ok(get(urlPathEqualTo("/v1/pms/hotel")), 200, "pms/hotel")
                .ok(get(urlPathEqualTo("/v1/pms/rooms")), 200, "pms/rooms")
                .ok(get(urlPathEqualTo("/v1/pms/reservations")), 200, "pms/reservations-in-house")
                .ok(get(urlPathEqualTo("/v1/pms/reservations/R-900641")), 200, "pms/reservation")
                .ok(get(urlPathEqualTo("/v1/pms/reservations/R-900641/folio")), 200, "pms/folio")
                .ok(get(urlPathEqualTo("/v1/pms/forecast")), 200, "pms/forecast");

        assertThat(pms.hotel().getRoomTypes()).isNotEmpty();
        assertThat(pms.rooms(null, null)).isNotEmpty().allSatisfy(room -> assertThat(room.getStatus()).isNotNull());
        List<Reservation> inHouse = pms.reservations(ReservationQuery.inHouse());
        assertThat(inHouse).isNotEmpty().allSatisfy(r -> assertThat(r.getGuest().getId()).isNotNull());
        assertThat(pms.reservation("R-900641").getGuest().getAllergies()).isNotNull();
        assertThat(pms.folio("R-900641").getLines()).isNotEmpty();
        assertThat(pms.forecast(LocalDate.now(), LocalDate.now().plusDays(13))).hasSize(14);
    }

    @Test
    void shouldReadPosFixtures() {
        stubs.posLogin()
                .ok(get(urlPathEqualTo("/v1/pos/dict/outlets")), 200, "pos/outlets")
                .ok(get(urlPathEqualTo("/v1/pos/dict/menu")), 200, "pos/menu-3")
                .ok(get(urlPathEqualTo("/v1/pos/dict/pms/lookup")), 200, "pos/lookup")
                .ok(get(urlPathEqualTo("/v1/pos/checks")), 200, "pos/checks-closed")
                .ok(get(urlPathEqualTo("/v1/pos/checks/1099")), 200, "pos/check-paid");

        assertThat(pos.outlets(null)).allSatisfy(outlet -> assertThat(outlet.getTenders()).isNotEmpty());
        List<MenuItemDTO> menu = pos.menu(3, null);
        assertThat(menu).isNotEmpty().allSatisfy(item -> assertThat(item.getAllergens()).isNotNull());
        assertThat(pos.lookup(3, "205")).isNotEmpty();
        assertThat(pos.checks(CheckQuery.open())).isNotEmpty().allSatisfy(c -> assertThat(c.getCheckSeq()).isNotNull());
        CheckDTO paid = pos.check(1099, 3, 83334L);
        assertThat(paid.getPayments()).isNotEmpty();
    }

    @Test
    void shouldReadEventsFixture() {
        stubs.ok(get(urlPathEqualTo("/v1/events")), 200, "events/events");

        EventListResponse events = platform.events(0, 100, null);

        assertThat(events.getElements()).isNotEmpty()
                .allSatisfy(event -> assertThat(event.getKey()).isNotBlank());
        assertThat(events.getNextSince()).isPositive();
    }

    @Test
    void shouldReadRoomPatchFixture() {
        stubs.ok(com.github.tomakehurst.wiremock.client.WireMock.patch(urlPathEqualTo("/v1/pms/rooms/205")),
                200, "pms/room-patched");

        Room room = pms.updateRoom("205", new com.hackathon.team4.sandbox.generated.model.RoomPatch());

        assertThat(room.getNumber()).isEqualTo("205");
    }
}
