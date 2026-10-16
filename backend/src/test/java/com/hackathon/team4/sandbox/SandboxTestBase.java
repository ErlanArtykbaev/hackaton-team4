package com.hackathon.team4.sandbox;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Spring context with WireMock in place of the sandbox. One server for the whole test run, so the cached Spring
 * context keeps a valid base URL; stubs and the POS token are reset before every test.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class SandboxTestBase {

    protected static final WireMockServer SANDBOX = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        SANDBOX.start();
    }

    protected final SandboxStubs stubs = new SandboxStubs(SANDBOX);

    @Autowired
    private PosTokenProvider posTokens;

    @DynamicPropertySource
    static void sandboxUrl(DynamicPropertyRegistry registry) {
        registry.add("sandbox.base-url", SANDBOX::baseUrl);
    }

    @BeforeEach
    void resetSandbox() {
        SANDBOX.resetAll();
        posTokens.invalidate();
    }
}
