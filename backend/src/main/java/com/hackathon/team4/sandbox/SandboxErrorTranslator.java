package com.hackathon.team4.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Turns client exceptions into {@link SandboxException} using the {@code code} of the contract's error envelope.
 */
class SandboxErrorTranslator {

    private final ObjectMapper mapper;

    SandboxErrorTranslator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    SandboxException translate(RestClientException exception) {
        if (exception instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            SandboxErrorCode code = codeOf(response.getResponseBodyAsString());
            return new SandboxException(code, status, "Sandbox answered " + status + " " + code, exception);
        }
        if (exception instanceof ResourceAccessException) {
            return new SandboxException(SandboxErrorCode.NETWORK_ERROR, 0, "Sandbox not reachable", exception);
        }
        return new SandboxException(SandboxErrorCode.UNKNOWN, 0, "Sandbox call failed", exception);
    }

    private SandboxErrorCode codeOf(String body) {
        if (body == null || body.isBlank()) {
            return SandboxErrorCode.UNKNOWN;
        }
        try {
            JsonNode code = mapper.readTree(body).path("code");
            return code.isTextual() ? SandboxErrorCode.fromContract(code.asText()) : SandboxErrorCode.UNKNOWN;
        } catch (IOException notJson) {
            return SandboxErrorCode.UNKNOWN;
        }
    }
}
