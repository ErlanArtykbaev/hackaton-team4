package com.hackathon.team4.sandbox;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Stub setup for the sandbox, backed by the fixtures recorded from the practice hotel
 * ({@code src/test/resources/sandbox/}, see {@code ai-rules/TESTING.md} §3).
 */
public class SandboxStubs {

    public static final String RATE_LIMITED_BODY =
            "{\"success\":false,\"message\":\"Rate limit exceeded\",\"code\":\"RATE_LIMITED\"}";
    public static final String UNAVAILABLE_BODY =
            "{\"success\":false,\"message\":\"Sandbox temporarily unavailable\",\"code\":\"SERVICE_UNAVAILABLE\"}";

    private final WireMockServer server;

    public SandboxStubs(WireMockServer server) {
        this.server = server;
    }

    /** The content of a recorded fixture, e.g. {@code fixture("pms/hotel")}. */
    public static String fixture(String name) {
        String path = "/sandbox/" + name + ".json";
        try (InputStream in = SandboxStubs.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("No fixture " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ResponseDefinitionBuilder json(int status, String body) {
        return aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body);
    }

    /** Answers {@code request} with a recorded fixture. */
    public SandboxStubs ok(MappingBuilder request, int status, String fixtureName) {
        server.stubFor(request.willReturn(json(status, fixture(fixtureName))));
        return this;
    }

    /** Answers {@code request} with an error every time. */
    public SandboxStubs failWith(MappingBuilder request, int status, String body) {
        server.stubFor(request.willReturn(json(status, body)));
        return this;
    }

    /**
     * First answer is the error, every later answer is the fixture. {@code request} builds a fresh matcher for each
     * of the two stubs (a WireMock builder cannot be registered twice).
     */
    public SandboxStubs failOnceThenOk(Supplier<MappingBuilder> request, int errorStatus, String errorBody,
                                       int okStatus, String fixtureName) {
        String scenario = "fail-once-" + System.nanoTime();
        server.stubFor(request.get().inScenario(scenario).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(json(errorStatus, errorBody)).willSetStateTo("recovered"));
        server.stubFor(request.get().inScenario(scenario).whenScenarioStateIs("recovered")
                .willReturn(json(okStatus, fixture(fixtureName))));
        return this;
    }

    public SandboxStubs posLogin() {
        return ok(post(urlPathEqualTo("/v1/auth/token")), 201, "pos/token");
    }
}
