package com.hackathon.team4.order;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.hackathon.team4.TestParty;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.sandbox.SandboxTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Orders over the stubbed POS. The menu cache lives as long as the shared Spring context, so every test uses its own
 * rvc; the menu fixture is the practice hotel's.
 */
@AutoConfigureMockMvc
class OrderControllerTest extends SandboxTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PassportService passports;

    @Test
    void shouldRefuseOrderWithoutGuestConsentAndSendNothingToThePos() throws Exception {
        TestParty party = TestParty.save(passports, SANDBOX, "R-990301");
        stubs.posLogin().ok(WireMock.get(urlPathEqualTo("/v1/pos/dict/menu")), 200, "pos/menu-2");

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rvc": 45, "reservationId": "%s", "table": "P3", "waiterCode": "W02", "lines": [
                          {"code": "2014", "quantity": 1, "condimentCodes": ["7021"], "forMemberId": "%s",
                           "waiterConfirmed": false}
                        ]}""".formatted(party.reservationId(), party.anna())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("GUEST_CONSENT_REQUIRED"))
                .andExpect(jsonPath("$.detail").value(
                        "Line 1 (Espresso) needs the guest's consent: MILK for Anna (intolerance)"));
        SANDBOX.verify(0, postRequestedFor(urlPathEqualTo("/v1/pos/checks")));
    }
}
