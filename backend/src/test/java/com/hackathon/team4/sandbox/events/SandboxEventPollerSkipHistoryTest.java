package com.hackathon.team4.sandbox.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackathon.team4.sandbox.SandboxTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.hackathon.team4.sandbox.SandboxStubs.fixture;
import static com.hackathon.team4.sandbox.SandboxStubs.json;
import static org.assertj.core.api.Assertions.assertThat;

@RecordApplicationEvents
@TestPropertySource(properties = "sandbox.events.skip-history=true")
class SandboxEventPollerSkipHistoryTest extends SandboxTestBase {

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
    void shouldSkipFeedHistoryOnFreshDatabaseAndPublishOnlyNewEvents() throws Exception {
        JsonNode history = mapper.readTree(fixture("events/events"));
        long end = history.get("nextSince").asLong();
        ObjectNode fresh = history.get("elements").get(0).deepCopy();
        fresh.put("id", end + 1);
        fresh.put("key", "pos.check.round_sent:4:9999:1:1");
        String live = """
                {"success": true, "message": null, "elements": [%s], "nextSince": %d}
                """.formatted(fresh, end + 1);
        SANDBOX.stubFor(get(urlPathEqualTo("/v1/events")).withQueryParam("since", equalTo("0"))
                .willReturn(json(200, history.toString())));
        SANDBOX.stubFor(get(urlPathEqualTo("/v1/events")).withQueryParam("since", equalTo(String.valueOf(end)))
                .willReturn(json(200, live)));

        int skippedRun = poller.pollOnce();

        assertThat(skippedRun).isZero();
        assertThat(applicationEvents.stream(SandboxEventReceived.class)).isEmpty();
        assertThat(cursors.findById(EventCursor.FEED)).get().extracting(EventCursor::nextSince).isEqualTo(end);

        int liveRun = poller.pollOnce();

        assertThat(liveRun).isEqualTo(1);
        assertThat(applicationEvents.stream(SandboxEventReceived.class)).hasSize(1);
    }
}
