package com.hackathon.team4.sandbox.events;

import com.hackathon.team4.sandbox.SandboxCalls;
import com.hackathon.team4.sandbox.generated.api.PlatformApi;
import com.hackathon.team4.sandbox.generated.model.EventListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * The events feed and the hotel reset.
 */
@Component
public class PlatformGateway {

    private final PlatformApi api;
    private final PlatformApi resetApi;
    private final SandboxCalls calls;

    PlatformGateway(@Qualifier("platformApi") PlatformApi api,
                    @Qualifier("platformResetApi") PlatformApi resetApi,
                    SandboxCalls calls) {
        this.api = api;
        this.resetApi = resetApi;
        this.calls = calls;
    }

    /** Events with {@code id > since}, oldest first. {@code types}: comma-separated, {@code null} = all. */
    public EventListResponse events(long since, int limit, String types) {
        return calls.safe(() -> api.listEvents(since, limit, types));
    }

    /**
     * Restores the hotel to its seed state. Allowed once per minute; never called from a feature
     * ({@code CLAUDE.md} §5), only from a manual demo-preparation action.
     */
    public void reset() {
        calls.once(resetApi::resetSandbox);
    }
}
