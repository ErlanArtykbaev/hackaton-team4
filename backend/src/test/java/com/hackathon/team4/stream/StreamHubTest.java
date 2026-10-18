package com.hackathon.team4.stream;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(StreamController.class)
@Import(StreamHub.class)
class StreamHubTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private StreamHub hub;

    @Test
    void shouldOpenStreamWithComment() throws Exception {
        MvcResult screen = openScreen();

        assertThat(screen.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(screen.getResponse().getContentAsString()).startsWith(":connected");
    }

    @Test
    void shouldSendEventToEveryOpenScreen() throws Exception {
        MvcResult kitchen = openScreen();
        MvcResult manager = openScreen();

        hub.broadcast(StreamEvent.LOG, Map.of("type", "GUEST_CONSENT"));

        // the format frontend/src/shared/stream.ts listens for: a named event with JSON data
        String event = "event:log\ndata:{\"type\":\"GUEST_CONSENT\"}\n\n";
        assertThat(kitchen.getResponse().getContentAsString()).contains(event);
        assertThat(manager.getResponse().getContentAsString()).contains(event);
    }

    private MvcResult openScreen() throws Exception {
        return mvc.perform(get("/api/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
    }
}
