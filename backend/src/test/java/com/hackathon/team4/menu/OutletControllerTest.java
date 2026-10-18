package com.hackathon.team4.menu;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.hackathon.team4.TestParty;
import com.hackathon.team4.common.error.GlobalExceptionHandler;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.sandbox.SandboxStubs;
import com.hackathon.team4.sandbox.SandboxTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The menu endpoint over the stubbed POS. The menu cache lives as long as the shared Spring context, so every test
 * uses its own rvc.
 */
@AutoConfigureMockMvc
class OutletControllerTest extends SandboxTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PassportService passports;

    @Test
    void shouldShowThePosMenuWithVerdictsForTheParty() throws Exception {
        TestParty party = TestParty.save(passports, SANDBOX, "R-990101");
        stubs.posLogin().ok(WireMock.get(urlPathEqualTo("/v1/pos/dict/menu")), 200, "pos/menu-2");

        mvc.perform(get("/api/outlets/42/menu?reservationId=" + party.reservationId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == '1020')].verdict").value("DANGER"))
                .andExpect(jsonPath("$[?(@.code == '1020')].dangerFor[0].memberName").value("Misha"))
                .andExpect(jsonPath("$[?(@.code == '1020')].safeAlternatives[0]").value("1023"))
                .andExpect(jsonPath("$[?(@.code == '2014')].condimentGroups[0].options[0].verdict")
                        .value("WARNING"));
        SANDBOX.verify(getRequestedFor(urlPathEqualTo("/v1/pos/dict/menu")).withQueryParam("rvc-on", equalTo("42")));
    }

    @Test
    void shouldAnswerUpstreamUnavailableWhenThePosMenuKeepsFailing() throws Exception {
        stubs.posLogin().failWith(WireMock.get(urlPathEqualTo("/v1/pos/dict/menu")), 503,
                SandboxStubs.UNAVAILABLE_BODY);

        mvc.perform(get("/api/outlets/43/menu").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(GlobalExceptionHandler.UPSTREAM_UNAVAILABLE));
    }
}
