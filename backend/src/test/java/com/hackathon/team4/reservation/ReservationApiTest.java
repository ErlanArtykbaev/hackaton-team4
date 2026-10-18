package com.hackathon.team4.reservation;

import com.hackathon.team4.common.error.GlobalExceptionHandler;
import com.hackathon.team4.sandbox.SandboxStubs;
import com.hackathon.team4.sandbox.SandboxTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reservation search and the front-desk worklist over WireMock: filtered PMS calls and a PMS failure.
 */
@AutoConfigureMockMvc
class ReservationApiTest extends SandboxTestBase {

    private static final String RESERVATIONS = "/v1/pms/reservations";

    @Autowired
    private MockMvc mvc;

    @Test
    void shouldFindInHouseGuestByRoomNumberInThePms() throws Exception {
        stubs.ok(get(urlPathEqualTo("/v1/pms/hotel")), 200, "pms/hotel")
                .ok(get(urlPathEqualTo(RESERVATIONS)).withQueryParam("roomNumber", equalTo("518"))
                        .withQueryParam("status", equalTo("IN_HOUSE")), 200, "pms/reservations-room-518")
                .ok(get(urlPathEqualTo(RESERVATIONS)).withQueryParam("roomNumber", equalTo("518"))
                        .withQueryParam("status", equalTo("BOOKED"))
                        .withQueryParam("arrivalFrom", equalTo("2026-10-16"))
                        .withQueryParam("arrivalTo", equalTo("2026-10-16")), 200, "pms/reservations-empty");

        mvc.perform(getJson("/api/reservations?query=518"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reservationId").value("R-203844"))
                .andExpect(jsonPath("$[0].guestName").value("Mikhail Kuznetsov"))
                .andExpect(jsonPath("$[0].children").value(1))
                .andExpect(jsonPath("$[0].language").value("ru"))
                .andExpect(jsonPath("$[0].pmsAllergens[0]").value("PEANUTS"));
    }

    @Test
    void shouldListTodaysArrivalsAndInHouseGuestsWithAllergiesFromThePms() throws Exception {
        stubs.ok(get(urlPathEqualTo("/v1/pms/hotel")), 200, "pms/hotel")
                .ok(get(urlPathEqualTo(RESERVATIONS)).withQueryParam("status", equalTo("BOOKED"))
                        .withQueryParam("arrivalFrom", equalTo("2026-10-16"))
                        .withQueryParam("arrivalTo", equalTo("2026-10-16")), 200, "pms/reservations-arrivals")
                .ok(get(urlPathEqualTo(RESERVATIONS)).withQueryParam("status", equalTo("IN_HOUSE")),
                        200, "pms/reservations-in-house");

        mvc.perform(getJson("/api/front-desk/arrivals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].reservationId", containsInAnyOrder(
                        "R-900667", "R-900668", "R-900670", "R-900649", "R-900652", "R-900661")));
    }

    @Test
    void shouldAnswerUpstreamUnavailableWhenThePmsKeepsFailing() throws Exception {
        stubs.ok(get(urlPathEqualTo("/v1/pms/hotel")), 200, "pms/hotel")
                .failWith(get(urlPathEqualTo(RESERVATIONS)), 503, SandboxStubs.UNAVAILABLE_BODY);

        mvc.perform(getJson("/api/reservations?query=Kuznetsov"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(GlobalExceptionHandler.UPSTREAM_UNAVAILABLE));
    }

    private static RequestBuilder getJson(String url) {
        return MockMvcRequestBuilders.get(url).accept(MediaType.APPLICATION_JSON);
    }
}
