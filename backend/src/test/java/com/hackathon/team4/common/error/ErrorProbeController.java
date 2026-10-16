package com.hackathon.team4.common.error;

import com.hackathon.team4.sandbox.SandboxErrorCode;
import com.hackathon.team4.sandbox.SandboxException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints that fail on purpose, to check the error format of our API.
 */
@RestController
class ErrorProbeController {

    record Probe(@NotBlank String name) {
    }

    @GetMapping("/test/sandbox-error/{code}")
    void sandboxError(@PathVariable SandboxErrorCode code) {
        throw new SandboxException(code, 400, "probe", null);
    }

    @GetMapping("/test/unexpected")
    void unexpected() {
        throw new IllegalStateException("probe");
    }

    @PostMapping("/test/validated")
    void validated(@Valid @RequestBody Probe probe) {
    }
}
