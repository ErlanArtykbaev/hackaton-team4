package com.hackathon.team4.sandbox;

import io.github.resilience4j.retry.Retry;
import java.util.function.Supplier;
import org.springframework.web.client.RestClientException;

/**
 * Executes sandbox calls: translates errors and retries only the calls that are safe to repeat
 * (see {@code ai-rules/BACKEND.md} §5.2).
 */
public class SandboxCalls {

    private final Retry retry;
    private final SandboxErrorTranslator translator;

    SandboxCalls(Retry retry, SandboxErrorTranslator translator) {
        this.retry = retry;
        this.translator = translator;
    }

    /** A call that may be repeated: GET, PATCH, or a write carrying a {@code postingGuid}. */
    public <T> T safe(Supplier<T> call) {
        return Retry.decorateSupplier(retry, () -> once(call)).get();
    }

    /** A call that must reach the sandbox at most once: opening a check, sending a round, resetting. */
    public <T> T once(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientException exception) {
            throw translator.translate(exception);
        }
    }

    public <T> T call(boolean repeatable, Supplier<T> call) {
        return repeatable ? safe(call) : once(call);
    }
}
