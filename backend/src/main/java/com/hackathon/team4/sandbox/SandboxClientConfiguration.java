package com.hackathon.team4.sandbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.team4.config.SandboxProperties;
import com.hackathon.team4.sandbox.generated.api.PlatformApi;
import com.hackathon.team4.sandbox.generated.api.PmsApi;
import com.hackathon.team4.sandbox.generated.api.PosApi;
import com.hackathon.team4.sandbox.generated.invoker.ApiClient;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * Wires the generated sandbox client: timeouts, the shared rate limiter, authentication and retry.
 */
@Configuration
class SandboxClientConfiguration {

    private final SandboxProperties properties;
    private final ObjectMapper mapper = ApiClient.createDefaultMapper(null);

    SandboxClientConfiguration(SandboxProperties properties) {
        this.properties = properties;
    }

    @Bean
    RateLimiter sandboxRateLimiter() {
        return RateLimiter.of("sandbox", RateLimiterConfig.custom()
                .limitForPeriod(properties.rateLimitPerSecond())
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(10))
                .build());
    }

    @Bean
    SandboxCalls sandboxCalls() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.retry().maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        properties.retry().initialBackoff(), 2.0, 0.5))
                .retryOnException(e -> e instanceof SandboxException failure && failure.code().isTransient())
                .build();
        return new SandboxCalls(Retry.of("sandbox", config), new SandboxErrorTranslator(mapper));
    }

    @Bean
    @Qualifier("sandbox")
    ApiClient sandboxApiClient(RateLimiter sandboxRateLimiter) {
        return apiClient(properties.readTimeout(), sandboxRateLimiter);
    }

    /** Separate client for reset, which may take up to 30 seconds. */
    @Bean
    @Qualifier("sandboxReset")
    ApiClient sandboxResetApiClient(RateLimiter sandboxRateLimiter) {
        return apiClient(properties.resetReadTimeout(), sandboxRateLimiter);
    }

    @Bean
    PmsApi pmsApi(@Qualifier("sandbox") ApiClient client) {
        return new PmsApi(client);
    }

    @Bean
    PosApi posApi(@Qualifier("sandbox") ApiClient client) {
        return new PosApi(client);
    }

    @Bean
    PlatformApi platformApi(@Qualifier("sandbox") ApiClient client) {
        return new PlatformApi(client);
    }

    @Bean
    PlatformApi platformResetApi(@Qualifier("sandboxReset") ApiClient client) {
        return new PlatformApi(client);
    }

    @Bean
    PosTokenProvider posTokenProvider(PosApi posApi, SandboxCalls calls) {
        PosTokenProvider tokens = new PosTokenProvider(posApi, properties, calls);
        posApi.getApiClient().setBearerToken(tokens::token);
        return tokens;
    }

    private ApiClient apiClient(Duration readTimeout, RateLimiter rateLimiter) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        ApiClient client = new ApiClient(
                ApiClient.buildRestClientBuilder(mapper)
                        .requestFactory(requestFactory)
                        .requestInterceptor(new RateLimitingInterceptor(rateLimiter))
                        .build(),
                mapper,
                ApiClient.createDefaultDateFormat());
        client.setBasePath(properties.baseUrl());
        client.setApiKey(properties.apiKey());
        return client;
    }
}
