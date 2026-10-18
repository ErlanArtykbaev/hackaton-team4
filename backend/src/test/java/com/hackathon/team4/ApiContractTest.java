package com.hackathon.team4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.common.error.GlobalExceptionHandler;
import com.hackathon.team4.kitchen.KitchenTicket;
import com.hackathon.team4.kitchen.KitchenTicketRepository;
import com.hackathon.team4.kitchen.TicketLine;
import com.hackathon.team4.kitchen.TicketSource;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogService;
import com.hackathon.team4.sandbox.SandboxStubs;
import com.hackathon.team4.sandbox.SandboxTestBase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The API contract of {@code PLAN.md} through the running application, with WireMock in place of the sandbox.
 */
@AutoConfigureMockMvc
class ApiContractTest extends SandboxTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private KitchenTicketRepository tickets;

    @Autowired
    private SafetyLogService safetyLog;

    @Autowired
    private PassportService passports;

    @Test
    void shouldListEveryContractEndpointInOpenApi() throws Exception {
        String body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode paths = json.readTree(body).path("paths");

        Map<String, List<String>> contract = Map.ofEntries(
                Map.entry("/api/reservations", List.of("get")),
                Map.entry("/api/front-desk/arrivals", List.of("get")),
                Map.entry("/api/passports/{reservationId}", List.of("get", "put")),
                Map.entry("/api/outlets", List.of("get")),
                Map.entry("/api/outlets/{rvc}/menu", List.of("get")),
                Map.entry("/api/orders/check", List.of("post")),
                Map.entry("/api/orders", List.of("post")),
                Map.entry("/api/kitchen/{rvc}/tickets", List.of("get")),
                Map.entry("/api/safety-log", List.of("get")),
                Map.entry("/api/safety-log/summary", List.of("get")),
                Map.entry("/api/stream", List.of("get")));
        contract.forEach((path, methods) -> methods.forEach(method ->
                assertThat(paths.path(path).has(method)).as("%s %s", method, path).isTrue()));
    }

    @Test
    void shouldFindDemoReservationByRoomNumber() throws Exception {
        stubs.ok(WireMock.get(WireMock.urlPathEqualTo("/v1/pms/hotel")), 200, "pms/hotel")
                .ok(WireMock.get(WireMock.urlPathEqualTo("/v1/pms/reservations"))
                        .withQueryParam("status", WireMock.equalTo("IN_HOUSE")), 200, "pms/reservations-room-518")
                .ok(WireMock.get(WireMock.urlPathEqualTo("/v1/pms/reservations"))
                        .withQueryParam("status", WireMock.equalTo("BOOKED")), 200, "pms/reservations-empty");

        mvc.perform(getJson("/api/reservations?query=518"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reservationId").value("R-203844"))
                .andExpect(jsonPath("$[0].pmsAllergens[0]").value("PEANUTS"));
    }

    @Test
    void shouldAskConsentFromAllergicMemberAndConfirmationForAnotherMember() throws Exception {
        TestParty party = TestParty.save(passports, SANDBOX, "R-990201");
        stubs.posLogin().ok(WireMock.get(WireMock.urlPathEqualTo("/v1/pos/dict/menu")), 200, "pos/menu-2");
        String order = """
                {"rvc": 44, "reservationId": "%s", "table": "P3", "waiterCode": "W02", "lines": [
                  {"code": "1023", "quantity": 1, "condimentCodes": [], "forMemberId": "%s", "waiterConfirmed": false},
                  {"code": "1020", "quantity": 1, "condimentCodes": [], "forMemberId": "%s", "waiterConfirmed": true},
                  {"code": "2014", "quantity": 1, "condimentCodes": ["7021"], "forMemberId": "%s",
                   "waiterConfirmed": false}
                ]}""".formatted(party.reservationId(), party.misha(), party.mikhail(), party.anna());

        mvc.perform(post("/api/orders/check").contentType(MediaType.APPLICATION_JSON).content(order))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].requirement").value("NONE"))
                .andExpect(jsonPath("$.lines[1].verdict").value("DANGER"))
                .andExpect(jsonPath("$.lines[1].dangerFor[0].memberName").value("Misha"))
                .andExpect(jsonPath("$.lines[1].requirement").value("WAITER_CONFIRMATION"))
                .andExpect(jsonPath("$.lines[2].verdict").value("WARNING"))
                .andExpect(jsonPath("$.lines[2].requirement").value("GUEST_CONSENT"));
    }

    @Test
    void shouldRejectInvalidOrderWithValidationError() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rvc\": 0, \"waiterCode\": \"\", \"lines\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(GlobalExceptionHandler.VALIDATION_ERROR));
    }

    @Test
    void shouldShowPrimaryGuestWhenNoPassportIsSaved() throws Exception {
        stubs.ok(WireMock.get(WireMock.urlPathEqualTo("/v1/pms/reservations/R-900641")), 200, "pms/reservation");

        mvc.perform(getJson("/api/passports/R-900641"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestName").value("Szymon Kowalski"))
                .andExpect(jsonPath("$.updatedAt").value(nullValue()))
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.members[0].primary").value(true))
                .andExpect(jsonPath("$.members[0].id").isString());
    }

    @Test
    void shouldAnswerNotFoundForUnknownReservation() throws Exception {
        stubs.failWith(WireMock.get(WireMock.urlPathEqualTo("/v1/pms/reservations/R-404")), 404,
                SandboxStubs.fixture("errors/404-not-found"));

        mvc.perform(getJson("/api/passports/R-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldSendKitchenTicketWithNumericCheckNumber() throws Exception {
        Instant now = Instant.now();
        TicketLine padThai = new TicketLine("Pad thai", 2, List.of(), List.of(Allergen.PEANUTS), Verdict.UNKNOWN,
                List.of(), null, false, null);
        tickets.save(new KitchenTicket(91, "1017", "88231", 1, "P1", null, null, null, now, TicketSource.UNLINKED,
                List.of(padThai), true, now));

        mvc.perform(getJson("/api/kitchen/91/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].checkNumber").value(1017))
                .andExpect(jsonPath("$[0].source").value("UNLINKED"))
                .andExpect(jsonPath("$[0].lines[0].allergens[0]").value("PEANUTS"));
    }

    @Test
    void shouldFindSafetyLogEntryByRoomNumber() throws Exception {
        safetyLog.append(safetyLog.entry(LogEntryType.GUEST_CONSENT)
                .outlet(4, "Pool Bar")
                .reservation("R-999905", "905", "Nina Ivanova")
                .member("Nina")
                .item("Thai iced tea")
                .allergen(Allergen.MILK, Severity.INTOLERANCE)
                .actor("W02")
                .statement("Nina Ivanova: I know it contains MILK and I order it")
                .build());

        mvc.perform(getJson("/api/safety-log?query=905"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("GUEST_CONSENT"))
                .andExpect(jsonPath("$[0].roomNumber").value("905"))
                .andExpect(jsonPath("$[0].severity").value("INTOLERANCE"));
    }

    private static RequestBuilder getJson(String url) {
        return get(url).accept(MediaType.APPLICATION_JSON);
    }
}
