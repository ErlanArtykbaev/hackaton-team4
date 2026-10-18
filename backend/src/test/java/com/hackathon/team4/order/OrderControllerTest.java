package com.hackathon.team4.order;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.hackathon.team4.TestParty;
import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.common.error.GlobalExceptionHandler;
import com.hackathon.team4.kitchen.KitchenTicket;
import com.hackathon.team4.kitchen.KitchenTicketRepository;
import com.hackathon.team4.kitchen.TicketLine;
import com.hackathon.team4.kitchen.TicketSource;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogEntry;
import com.hackathon.team4.safetylog.SafetyLogService;
import com.hackathon.team4.sandbox.SandboxStubs;
import com.hackathon.team4.sandbox.SandboxTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Orders over the stubbed POS. The menu cache lives as long as the shared Spring context, so every test uses its own
 * rvc; the menu, outlets and check fixtures are the practice hotel's.
 */
@AutoConfigureMockMvc
class OrderControllerTest extends SandboxTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PassportService passports;

    @Autowired
    private KitchenTicketRepository tickets;

    @Autowired
    private SafetyLogService safetyLog;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void shouldOpenALinkedPosCheckAndRecordTheKitchenTicketAndTheSafetyLog() throws Exception {
        TestParty party = TestParty.save(passports, SANDBOX, "R-990302");
        posWithMenuAndOutlets().ok(WireMock.post(urlPathEqualTo("/v1/pos/checks")), 201, "pos/check-opened");

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rvc": 2, "reservationId": "%s", "table": "B4", "waiterCode": "W02", "lines": [
                          {"code": "1023", "quantity": 2, "condimentCodes": [], "forMemberId": "%s",
                           "waiterConfirmed": false},
                          {"code": "1020", "quantity": 1, "condimentCodes": [], "forMemberId": "%s",
                           "waiterConfirmed": true},
                          {"code": "2014", "quantity": 1, "condimentCodes": ["7021"], "forMemberId": "%s",
                           "waiterConfirmed": false, "consent": {"givenBy": "Anna Kuznetsova"}}
                        ]}""".formatted(party.reservationId(), party.misha(), party.mikhail(), party.anna())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkNumber").value(1099))
                .andExpect(jsonPath("$.checkSeq").value(83334))
                .andExpect(jsonPath("$.lines[1].requirement").value("WAITER_CONFIRMATION"))
                .andExpect(jsonPath("$.lines[2].requirement").value("GUEST_CONSENT"));

        SANDBOX.verify(exactly(1), postRequestedFor(urlPathEqualTo("/v1/pos/checks"))
                .withQueryParam("rvc-on", equalTo("2"))
                .withRequestBody(matchingJsonPath("$.reservationId", equalTo(party.reservationId())))
                .withRequestBody(matchingJsonPath("$.table", equalTo("B4")))
                .withRequestBody(matchingJsonPath("$.orderType", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.details[0].quantity", equalTo("2")))
                .withRequestBody(matchingJsonPath("$.details[2].condimentItems[0].code", equalTo("7021"))));

        // the round as the POS numbered it, so the pos.check.round_sent feed finds the same ticket
        List<TicketLine> lines = transactions.execute(status -> {
            KitchenTicket ticket = tickets.findByRvcAndCheckSeqAndRound(2, "83334", 1).orElseThrow();
            assertThat(ticket.source()).isEqualTo(TicketSource.CHECKED);
            assertThat(ticket.checkNumber()).isEqualTo("1099");
            return List.copyOf(ticket.lines());
        });
        assertThat(lines).extracting(TicketLine::forMemberName, TicketLine::waiterConfirmed, TicketLine::consentBy)
                .containsExactly(
                        tuple("Misha", false, null),
                        tuple("Mikhail", true, null),
                        tuple("Anna", false, "Anna Kuznetsova"));
        assertThat(lines.get(2).condiments()).containsExactly("Whole milk");
        assertThat(lines.get(2).allergens()).containsExactly(Allergen.MILK);

        List<SafetyLogEntry> entries = safetyLog.search(null, 2, null).stream()
                .filter(entry -> party.reservationId().equals(entry.reservationId()))
                .toList();
        assertThat(entries).extracting(SafetyLogEntry::type).containsExactlyInAnyOrder(
                LogEntryType.ORDER_PLACED, LogEntryType.WAITER_CONFIRMED, LogEntryType.GUEST_CONSENT);
        assertThat(entries).filteredOn(entry -> entry.type() == LogEntryType.GUEST_CONSENT).singleElement()
                .satisfies(consent -> {
                    assertThat(consent.memberName()).isEqualTo("Anna");
                    assertThat(consent.item()).isEqualTo("Espresso with Whole milk");
                    assertThat(consent.severity()).isEqualTo(Severity.INTOLERANCE);
                    assertThat(consent.actor()).isEqualTo("W02");
                    assertThat(consent.statement())
                            .isEqualTo("Anna Kuznetsova: I know it contains MILK and I order it");
                });
    }

    @Test
    void shouldNotRetryOpeningTheCheckAndRecordNothingWhenThePosFails() throws Exception {
        TestParty party = TestParty.save(passports, SANDBOX, "R-990303");
        posWithMenuAndOutlets().failWith(WireMock.post(urlPathEqualTo("/v1/pos/checks")), 503,
                SandboxStubs.UNAVAILABLE_BODY);

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rvc": 1, "reservationId": "%s", "table": "T2", "waiterCode": "W02", "lines": [
                          {"code": "1023", "quantity": 1, "condimentCodes": [], "forMemberId": "%s",
                           "waiterConfirmed": false}
                        ]}""".formatted(party.reservationId(), party.misha())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(GlobalExceptionHandler.UPSTREAM_UNAVAILABLE));

        SANDBOX.verify(exactly(1), postRequestedFor(urlPathEqualTo("/v1/pos/checks")));
        assertThat(safetyLog.search(null, 1, null))
                .noneMatch(entry -> party.reservationId().equals(entry.reservationId()));
    }

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

    private SandboxStubs posWithMenuAndOutlets() {
        return stubs.posLogin()
                .ok(WireMock.get(urlPathEqualTo("/v1/pos/dict/menu")), 200, "pos/menu-2")
                .ok(WireMock.get(urlPathEqualTo("/v1/pos/dict/outlets")), 200, "pos/outlets");
    }
}
