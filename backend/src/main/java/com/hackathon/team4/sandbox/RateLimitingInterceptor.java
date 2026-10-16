package com.hackathon.team4.sandbox;

import io.github.resilience4j.ratelimiter.RateLimiter;
import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Keeps every request of the application below the sandbox limit, so we never trigger its 429.
 */
class RateLimitingInterceptor implements ClientHttpRequestInterceptor {

    private final RateLimiter rateLimiter;

    RateLimitingInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        RateLimiter.waitForPermission(rateLimiter);
        return execution.execute(request, body);
    }
}
