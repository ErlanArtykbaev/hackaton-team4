package com.hackathon.team4.stream;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.kitchen.KitchenTicketService;
import com.hackathon.team4.kitchen.KitchenTicketService.Round;
import com.hackathon.team4.kitchen.KitchenTicketService.RoundLine;
import com.hackathon.team4.kitchen.dto.KitchenTicketResponse;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.passport.PassportSnapshot;
import com.hackathon.team4.passport.dto.PassportResponse;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogService;
import com.hackathon.team4.safetylog.dto.LogEntry;
import com.hackathon.team4.sandbox.SandboxStubs;
import com.hackathon.team4.sandbox.SandboxTestBase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * BE-B's services publish their events; the screens get them through the stream once the change is committed.
 */
class StreamForwarderTest extends SandboxTestBase {

    @MockitoBean
    private StreamHub hub;

    @Autowired
    private SafetyLogService safetyLog;

    @Autowired
    private KitchenTicketService tickets;

    @Autowired
    private PassportService passports;

    @Test
    void shouldSendAppendedLogEntryToScreens() {
        safetyLog.append(safetyLog.entry(LogEntryType.GUEST_CONSENT)
                .reservation("R-999906", "906", "Nina Ivanova")
                .allergen(Allergen.MILK, Severity.INTOLERANCE)
                .statement("Nina Ivanova: I know it contains MILK and I order it")
                .build());

        verify(hub).broadcast(eq(StreamEvent.LOG),
                argThat(payload -> payload instanceof LogEntry entry && "906".equals(entry.roomNumber())));
    }

    @Test
    void shouldSendNewKitchenTicketToScreens() {
        RoundLine padThai = RoundLine.fromPos("Pad thai", 1, List.of(), Set.of(Allergen.PEANUTS));
        tickets.recordPosRound(new Round(92, "Pool Bar", "2042", "99042", 1, "P2", null, Instant.now(),
                List.of(padThai)));

        verify(hub).broadcast(eq(StreamEvent.TICKET),
                argThat(payload -> payload instanceof KitchenTicketResponse ticket && ticket.checkNumber() == 2042));
    }

    @Test
    void shouldSendSavedPassportToScreens() {
        // its own reservation and guest: the in-memory database is shared with the other application tests
        String reservation = SandboxStubs.fixture("pms/reservation").replace("900641", "900642");
        SANDBOX.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/pms/reservations/R-900642"))
                .willReturn(SandboxStubs.json(200, reservation)));
        PassportSnapshot.Member primary = new PassportSnapshot.Member(null, "Szymon Kowalski", null, true, Map.of());

        passports.save("R-900642", null, List.of(primary), "Front desk", PassportService.Source.FRONT_DESK, null, null);

        verify(hub).broadcast(eq(StreamEvent.PASSPORT),
                argThat(payload -> payload instanceof PassportResponse passport
                        && "R-900642".equals(passport.reservationId())));
    }
}
