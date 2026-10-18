package com.hackathon.team4;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.kitchen.KitchenTicketService;
import com.hackathon.team4.kitchen.KitchenTicketService.Round;
import com.hackathon.team4.kitchen.KitchenTicketService.RoundLine;
import com.hackathon.team4.sandbox.SandboxStubs;
import com.hackathon.team4.sandbox.SandboxTestBase;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * The demo path (PLAN.md) through our API, with WireMock as the sandbox: Kuznetsov family, room 518, Pool Bar.
 * Steps 2–3 (menu verdicts, order placement) and the event feed of step 4 belong to BE-A4/BE-A5; here the kitchen
 * rounds go through {@link KitchenTicketService} the way those will call it.
 */
@AutoConfigureMockMvc
@Transactional
class DemoPathTest extends SandboxTestBase {

    private static final String RESERVATION = "R-203844";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private KitchenTicketService kitchen;

    @Test
    void shouldRecordPassportOrdersAndProofForTheKuznetsovFamily() throws Exception {
        SANDBOX.stubFor(get(urlPathEqualTo("/v1/pms/reservations/" + RESERVATION)).willReturn(SandboxStubs.json(200,
                """
                {"success": true, "reservation": {"id": "R-203844", "status": "IN_HOUSE", "roomNumber": "518",
                  "guest": {"id": "G-203844", "firstName": "Mikhail", "lastName": "Kuznetsov",
                            "allergies": ["PEANUTS"], "preferences": [], "marketingConsent": false}}}
                """)));
        SANDBOX.stubFor(patch(urlPathEqualTo("/v1/pms/guests/G-203844")).willReturn(SandboxStubs.json(200,
                """
                {"success": true, "guest": {"id": "G-203844", "firstName": "Mikhail", "lastName": "Kuznetsov"}}
                """)));

        // the outlet name in the safety log comes from the POS
        stubs.posLogin();
        SANDBOX.stubFor(get(urlPathEqualTo("/v1/pos/dict/outlets")).willReturn(SandboxStubs.json(200, """
                {"success": true, "elements": [{"objectNumber": 4, "name": "Pool Bar", "kind": "BAR", "tables": ["P3"]}]}
                """)));

        // 1. Front desk: a card per person, Misha 7 with PEANUTS severe.
        JsonNode passport = save("""
                {"members": [{"name": "Mikhail", "primary": true, "allergies": []},
                             {"name": "Anna", "primary": false, "allergies": []},
                             {"name": "Misha", "age": 7, "primary": false,
                              "allergies": [{"allergen": "PEANUTS", "severity": "SEVERE"}]}],
                 "updatedBy": "front-desk", "source": "FRONT_DESK"}
                """);
        UUID mikhail = UUID.fromString(passport.at("/members/0/id").asText());
        UUID anna = UUID.fromString(passport.at("/members/1/id").asText());
        UUID misha = UUID.fromString(passport.at("/members/2/id").asText());

        // 2. Pool Bar waiter: Anna is lactose intolerant; the PMS gets the union.
        save("""
                {"members": [{"id": "%s", "name": "Mikhail", "primary": true, "allergies": []},
                             {"id": "%s", "name": "Anna", "primary": false,
                              "allergies": [{"allergen": "MILK", "severity": "INTOLERANCE"}]},
                             {"id": "%s", "name": "Misha", "age": 7, "primary": false,
                              "allergies": [{"allergen": "PEANUTS", "severity": "SEVERE"}]}],
                 "updatedBy": "W02", "source": "WAITER", "rvc": 4}
                """.formatted(mikhail, anna, misha));
        SANDBOX.verify(patchRequestedFor(urlPathEqualTo("/v1/pms/guests/G-203844"))
                .withRequestBody(equalToJson("""
                        {"allergies": ["MILK", "PEANUTS"]}
                        """, true, true)));

        // 3–4. The tablet round reaches the kitchen with for whom, the confirmation and the consent ...
        Instant sent = Instant.now();
        kitchen.recordCheckedRound(new Round(4, "Pool Bar", "1101", "83400", 1, "P3", RESERVATION, sent, List.of(
                new RoundLine("Kids nuggets", 1, List.of(), Set.of(Allergen.GLUTEN, Allergen.EGGS), misha, false,
                        null),
                new RoundLine("Chicken satay", 1, List.of(), Set.of(Allergen.PEANUTS), mikhail, true, null),
                new RoundLine("Thai iced tea", 1, List.of(), Set.of(Allergen.MILK), anna, false,
                        "Anna Kuznetsova"))));
        // ... then the bartender types Som tam for room 518 straight into the POS.
        kitchen.recordPosRound(new Round(4, "Pool Bar", "1102", "83401", 1, null, RESERVATION, sent.plusSeconds(30),
                List.of(RoundLine.fromPos("Som tam", 1, List.of(), Set.of(Allergen.PEANUTS, Allergen.FISH)))));

        mvc.perform(MockMvcRequestBuilders.get("/api/kitchen/4/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("UNCHECKED"))
                .andExpect(jsonPath("$[0].roomNumber").value("518"))
                .andExpect(jsonPath("$[0].lines[0].dangerFor[0].memberName").value("Misha"))
                .andExpect(jsonPath("$[0].lines[0].dangerFor[0].severity").value("SEVERE"))
                .andExpect(jsonPath("$[1].source").value("CHECKED"))
                .andExpect(jsonPath("$[1].lines[1].forMemberName").value("Mikhail"))
                .andExpect(jsonPath("$[1].lines[1].waiterConfirmed").value(true))
                .andExpect(jsonPath("$[1].lines[1].verdict").value("DANGER"))
                .andExpect(jsonPath("$[1].lines[2].verdict").value("WARNING"))
                .andExpect(jsonPath("$[1].lines[2].consentBy").value("Anna Kuznetsova"));

        // 5. The manager searches 518: the unchecked POS order and both passport changes, newest first.
        JsonNode log = json.readTree(mvc.perform(MockMvcRequestBuilders.get("/api/safety-log")
                        .param("query", "518"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(log.findValuesAsText("type"))
                .containsExactly("POS_ORDER_UNCHECKED", "PASSPORT_UPDATED", "PASSPORT_UPDATED");
        assertThat(log.at("/0/statement").asText())
                .isEqualTo("Not checked at ordering: Som tam — PEANUTS for Misha (severe)");
        assertThat(log.at("/1/actor").asText()).isEqualTo("W02");
        assertThat(log.at("/1/outletName").asText()).isEqualTo("Pool Bar");
        assertThat(log.at("/1/statement").asText()).isEqualTo("WAITER: Anna: +MILK (intolerance)");
        mvc.perform(MockMvcRequestBuilders.get("/api/safety-log/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uncheckedPosOrders").value(1));
    }

    private JsonNode save(String body) throws Exception {
        String response = mvc.perform(put("/api/passports/" + RESERVATION).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }
}
