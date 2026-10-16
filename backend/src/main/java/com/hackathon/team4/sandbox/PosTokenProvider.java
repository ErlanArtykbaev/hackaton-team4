package com.hackathon.team4.sandbox;

import com.hackathon.team4.config.SandboxProperties;
import com.hackathon.team4.sandbox.generated.api.PosApi;
import com.hackathon.team4.sandbox.generated.model.AuthToken;
import com.hackathon.team4.sandbox.generated.model.LoginDetailsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs in to the POS lazily with the configured PIN and caches the Bearer token until the sandbox rejects it.
 */
public class PosTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(PosTokenProvider.class);

    private final PosApi posApi;
    private final SandboxProperties properties;
    private final SandboxCalls calls;
    private String token;

    PosTokenProvider(PosApi posApi, SandboxProperties properties, SandboxCalls calls) {
        this.posApi = posApi;
        this.properties = properties;
        this.calls = calls;
    }

    public synchronized String token() {
        if (token == null) {
            token = login();
        }
        return token;
    }

    /** Forget the token; the next POS call logs in again. */
    public synchronized void invalidate() {
        token = null;
    }

    private String login() {
        LoginDetailsDTO login = new LoginDetailsDTO()
                .pinCode(properties.pin())
                .accessCode(properties.accessCode());
        AuthToken auth = calls.safe(() -> posApi.createToken(login, properties.deviceId()));
        log.info("Logged in to the POS as employee {}", auth.getCode());
        return auth.getToken();
    }
}
