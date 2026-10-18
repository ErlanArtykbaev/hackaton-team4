package com.hackathon.team4.passport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.kitchen.KitchenTicket;
import com.hackathon.team4.kitchen.KitchenTicketController;
import com.hackathon.team4.kitchen.KitchenTicketService;
import com.hackathon.team4.kitchen.TicketLine;
import com.hackathon.team4.kitchen.TicketSource;
import com.hackathon.team4.menu.OutletService;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogController;
import com.hackathon.team4.safetylog.SafetyLogEntry;
import com.hackathon.team4.safetylog.SafetyLogService;
import com.hackathon.team4.sandbox.SandboxErrorCode;
import com.hackathon.team4.sandbox.SandboxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** The passport, kitchen ticket and safety-log endpoints: mapping and request validation. */
@WebMvcTest({PassportController.class, KitchenTicketController.class, SafetyLogController.class})
class AllergyApiTest {

    private static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");
    private static final UUID ANNA = UUID.fromString("518a0000-0000-4000-8000-000000000002");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PassportService passports;

    @MockitoBean
    private OutletService outlets;

    @MockitoBean
    private KitchenTicketService kitchen;

    @MockitoBean
    private SafetyLogService safetyLog;

    @Test
    void shouldSaveWaitersPassportChangeWithTheOutletName() throws Exception {
        when(outlets.outletName(4)).thenReturn(Optional.of("Pool Bar"));
        when(passports.save(eq("R-203844"), isNull(), anyList(), eq("W02"), eq(PassportService.Source.WAITER),
                eq(4), eq("Pool Bar"))).thenReturn(new PassportSnapshot("R-203844", "518", "Mikhail Kuznetsov", null,
                List.of(Allergen.MILK), List.of(new PassportSnapshot.Member(ANNA, "Anna", null, false,
                        Map.of(Allergen.MILK, Severity.INTOLERANCE))), NOW, "W02"));

        mvc.perform(put("/api/passports/R-203844").contentType(MediaType.APPLICATION_JSON).content("""
                        {"members": [{"id": "%s", "name": "Anna", "primary": false,
                                      "allergies": [{"allergen": "MILK", "severity": "INTOLERANCE"}]}],
                         "updatedBy": "W02", "source": "WAITER", "rvc": 4}
                        """.formatted(ANNA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0].id").value(ANNA.toString()))
                .andExpect(jsonPath("$.members[0].allergies[0].severity").value("INTOLERANCE"))
                .andExpect(jsonPath("$.pmsAllergens[0]").value("MILK"))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-18T09:00:00Z"));
    }

    @Test
    void shouldRefusePmsAsSourceFromTheApi() throws Exception {
        mvc.perform(put("/api/passports/R-203844").contentType(MediaType.APPLICATION_JSON).content("""
                        {"members": [{"name": "Anna", "primary": true, "allergies": []}],
                         "updatedBy": "W02", "source": "PMS"}
                        """))
                .andExpect(status().isBadRequest());

        verify(passports, never()).save(any(), any(), anyList(), any(), any(), any(), any());
    }

    @Test
    void shouldRefuseCardWithoutName() throws Exception {
        mvc.perform(put("/api/passports/R-203844").contentType(MediaType.APPLICATION_JSON).content("""
                        {"members": [{"name": " ", "primary": true, "allergies": []}],
                         "updatedBy": "front-desk", "source": "FRONT_DESK"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldAnswerNotFoundForReservationThePmsDoesNotKnow() throws Exception {
        when(passports.get("R-000000")).thenThrow(new SandboxException(SandboxErrorCode.NOT_FOUND, 404, "no", null));

        mvc.perform(get("/api/passports/R-000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldListKitchenTicketsWithForWhomAndConsent() throws Exception {
        DangerFor anna = new DangerFor(ANNA, "Anna", Allergen.MILK, Severity.INTOLERANCE);
        when(kitchen.tickets(4)).thenReturn(List.of(new KitchenTicket(4, "1101", "83400", 1, "P3", "R-203844",
                "518", "Mikhail Kuznetsov", NOW, TicketSource.CHECKED, List.of(new TicketLine("Thai iced tea", 1,
                        List.of(), List.of(Allergen.MILK), Verdict.WARNING, List.of(anna), "Anna", false,
                        "Anna Kuznetsova")), true, NOW)));

        mvc.perform(get("/api/kitchen/4/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].checkNumber").value(1101))
                .andExpect(jsonPath("$[0].source").value("CHECKED"))
                .andExpect(jsonPath("$[0].hasAllergens").value(true))
                .andExpect(jsonPath("$[0].lines[0].dangerFor[0].memberName").value("Anna"))
                .andExpect(jsonPath("$[0].lines[0].consentBy").value("Anna Kuznetsova"));
    }

    @Test
    void shouldSearchSafetyLogByRoom() throws Exception {
        when(safetyLog.search("518", null, null)).thenReturn(List.of(SafetyLogEntry.of(LogEntryType.GUEST_CONSENT, NOW)
                .outlet(4, "Pool Bar").reservation("R-203844", "518", "Mikhail Kuznetsov").member("Anna")
                .item("Thai iced tea").allergen(Allergen.MILK, Severity.INTOLERANCE).actor("W02")
                .statement("I know it contains MILK and I order it").build()));

        mvc.perform(get("/api/safety-log").param("query", "518"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("GUEST_CONSENT"))
                .andExpect(jsonPath("$[0].outletName").value("Pool Bar"))
                .andExpect(jsonPath("$[0].severity").value("INTOLERANCE"))
                .andExpect(jsonPath("$[0].statement").value("I know it contains MILK and I order it"));
    }
}
