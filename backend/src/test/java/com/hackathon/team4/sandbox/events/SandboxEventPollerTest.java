package com.hackathon.team4.sandbox.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackathon.team4.sandbox.SandboxTestBase;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.hackathon.team4.sandbox.SandboxStubs.fixture;
import static com.hackathon.team4.sandbox.SandboxStubs.json;
import static org.assertj.core.api.Assertions.assertThat;

@RecordApplicationEvents
class SandboxEventPollerTest extends SandboxTestBase {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private SandboxEventPoller poller;

    @Autowired
    private EventCursorRepository cursors;

    @Autowired
    private ProcessedEventRepository processed;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void clearFeedState() {
        cursors.deleteAll();
        processed.deleteAll();
    }

    @Test
    void shouldPublishEachEventOnceAndStoreCursor() throws Exception {
        JsonNode feed = mapper.readTree(fixture("events/events"));
        long nextSince = feed.get("nextSince").asLong();
        Set<String> keys = new HashSet<>();
        feed.get("elements").forEach(event -> keys.add(event.get("key").asText()));
        SANDBOX.stubFor(get(urlPathEqualTo("/v1/events")).withQueryParam("since", equalTo("0"))
                .willReturn(json(200, feed.toString())));

        int published = poller.pollOnce();

        assertThat(published).isEqualTo(keys.size());
        assertThat(applicationEvents.stream(SandboxEventReceived.class)).hasSize(keys.size());
        assertThat(cursors.findById(EventCursor.FEED)).get()
                .extracting(EventCursor::nextSince).isEqualTo(nextSince);
    }

    @Test
    void shouldSkipRedeliveredEventWithSameKey() throws Exception {
        JsonNode feed = mapper.readTree(fixture("events/events"));
        long nextSince = feed.get("nextSince").asLong();
        ObjectNode duplicate = feed.get("elements").get(0).deepCopy();
        duplicate.put("id", nextSince + 1);
        String redelivery = """
                {"success": true, "message": null, "elements": [%s], "nextSince": %d}
                """.formatted(duplicate, nextSince + 1);
        SANDBOX.stubFor(get(urlPathEqualTo("/v1/events")).withQueryParam("since", equalTo("0"))
                .willReturn(json(200, feed.toString())));
        SANDBOX.stubFor(get(urlPathEqualTo("/v1/events")).withQueryParam("since", equalTo(String.valueOf(nextSince)))
                .willReturn(json(200, redelivery)));
        poller.pollOnce();

        int published = poller.pollOnce();

        assertThat(published).isZero();
        assertThat(cursors.findById(EventCursor.FEED)).get()
                .extracting(EventCursor::nextSince).isEqualTo(nextSince + 1);
    }
}
