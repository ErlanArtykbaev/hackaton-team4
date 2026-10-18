package com.hackathon.team4.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Connection to the hotel sandbox. Secrets come from {@code backend/.env}; defaults live in {@code application.yml}.
 */
@Validated
@ConfigurationProperties(prefix = "sandbox")
public record SandboxProperties(
        @NotBlank String baseUrl,
        String apiKey,
        String accessCode,
        String pin,
        @NotBlank String deviceId,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull Duration resetReadTimeout,
        @Min(1) int rateLimitPerSecond,
        @Valid @NotNull Retry retry,
        @Valid @NotNull Events events) {

    public record Retry(@Min(1) int maxAttempts, @NotNull Duration initialBackoff) {
    }

    /** {@code skipHistory}: with no stored cursor (fresh database), start at the end of the feed and publish nothing older. */
    public record Events(boolean enabled, @NotNull Duration pollInterval, @Min(1) int batchSize, boolean skipHistory) {
    }
}
