package com.hackathon.team4.stream;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Live updates for the staff screens.
 */
@RestController
public class StreamController {

    private final StreamHub hub;

    public StreamController(StreamHub hub) {
        this.hub = hub;
    }

    @GetMapping(path = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiResponse(responseCode = "200",
            description = "Server-sent events: ticket (KitchenTicket), log (LogEntry), passport (Passport)",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string")))
    public SseEmitter stream() {
        return hub.connect();
    }
}
