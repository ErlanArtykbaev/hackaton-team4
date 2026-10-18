package com.hackathon.team4.kitchen.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.kitchen.KitchenTicket;
import com.hackathon.team4.kitchen.KitchenTicketRepository;
import com.hackathon.team4.kitchen.TicketSource;
import com.hackathon.team4.sandbox.SandboxStubs;
import com.hackathon.team4.sandbox.SandboxTestBase;
import com.hackathon.team4.sandbox.events.SandboxEventReceived;
import com.hackathon.team4.sandbox.generated.model.Event;
import com.hackathon.team4.sandbox.generated.model.EventType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rounds from the event feed become kitchen tickets. Each test uses its own rvc and checkSeq: the in-memory database
 * is shared with the other application tests, and the menu cache lives as long as the context.
 */
class PosRoundListenerTest extends SandboxTestBase {

    @Autowired
    private PosRoundListener listener;

    @Autowired
    private KitchenTicketRepository tickets;

    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void posAnswers() {
        stubs.posLogin()
                .ok(WireMock.get(WireMock.urlPathEqualTo("/v1/pos/dict/outlets")), 200, "pos/outlets")
                .ok(WireMock.get(WireMock.urlPathEqualTo("/v1/pos/dict/menu"))
                        .withQueryParam("rvc-on", WireMock.equalTo("3")), 200, "pos/menu-3")
                .ok(WireMock.get(WireMock.urlPathEqualTo("/v1/pms/reservations/R-900641")), 200, "pms/reservation");
    }

    @Test
    void shouldRecordRecordedRoundWithTheKeyOfTheEvent() throws Exception {
        Event event = recordedRoundSent();

        listener.onSandboxEvent(new SandboxEventReceived(event));

        // key pos.check.round_sent:3:1098:83333:1 → rvc 3, checkSeq "83333", round 1
        assertThat(tickets.existsByRvcAndCheckSeqAndRound(3, "83333", 1)).isTrue();
        KitchenTicket ticket = ticket(3, "83333");
        assertThat(ticket.checkNumber()).isEqualTo("1098");
        assertThat(ticket.source()).isEqualTo(TicketSource.UNCHECKED);
        assertThat(ticket.lines()).extracting("name").containsExactly("Borjomi mineral water", "Espresso");
    }

    @Test
    void shouldAddAllergensOfChosenCondimentsFromTheMenu() {
        Map<String, Object> espressoWithMilk = Map.of("code", "2014", "name", "Espresso", "quantity", 1,
                "allergens", List.of(), "condiments", List.of(Map.of("code", "7021", "name", "Whole milk")));

        listener.onSandboxEvent(new SandboxEventReceived(roundSent(3, "99001", List.of(espressoWithMilk))));

        KitchenTicket ticket = ticket(3, "99001");
        assertThat(ticket.source()).isEqualTo(TicketSource.UNLINKED);
        assertThat(ticket.hasAllergens()).isTrue();
        assertThat(ticket.lines().get(0).condiments()).containsExactly("Whole milk");
        assertThat(ticket.lines().get(0).allergens()).containsExactly(Allergen.MILK);
    }

    @Test
    void shouldKeepTheEventsAllergensWhenTheMenuIsUnavailable() {
        stubs.failWith(WireMock.get(WireMock.urlPathEqualTo("/v1/pos/dict/menu"))
                .withQueryParam("rvc-on", WireMock.equalTo("2")), 503, SandboxStubs.UNAVAILABLE_BODY);
        Map<String, Object> satay = Map.of("code", "4101", "name", "Chicken satay", "quantity", 2,
                "allergens", List.of("PEANUTS"), "condiments", List.of());

        listener.onSandboxEvent(new SandboxEventReceived(roundSent(2, "99002", List.of(satay))));

        KitchenTicket ticket = ticket(2, "99002");
        assertThat(ticket.lines().get(0).quantity()).isEqualTo(2);
        assertThat(ticket.lines().get(0).allergens()).containsExactly(Allergen.PEANUTS);
    }

    private Event recordedRoundSent() throws Exception {
        JsonNode elements = json.readTree(SandboxStubs.fixture("events/events")).path("elements");
        JsonNode roundSent = StreamSupport.stream(elements.spliterator(), false)
                .filter(event -> "pos.check.round_sent".equals(event.path("type").asText()))
                .findFirst()
                .orElseThrow();
        return json.treeToValue(roundSent, Event.class);
    }

    private static Event roundSent(int rvc, String checkSeq, List<Map<String, Object>> items) {
        Map<String, Object> data = Map.of("rvc", rvc, "checkNumber", "2001", "checkSeq", checkSeq, "round", 1,
                "items", items);
        return new Event()
                .key("pos.check.round_sent:" + rvc + ":2001:" + checkSeq + ":1")
                .type(EventType.POS_CHECK_ROUND_SENT)
                .occurredAt(OffsetDateTime.now())
                .data(data);
    }

    private KitchenTicket ticket(int rvc, String checkSeq) {
        return tickets.findByRvcAndSentAtAfterOrderBySentAtDesc(rvc, Instant.EPOCH).stream()
                .filter(ticket -> ticket.checkSeq().equals(checkSeq))
                .findFirst()
                .orElseThrow();
    }
}
