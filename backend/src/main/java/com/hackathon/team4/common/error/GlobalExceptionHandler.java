package com.hackathon.team4.common.error;

import com.hackathon.team4.sandbox.SandboxErrorCode;
import com.hackathon.team4.sandbox.SandboxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every error of our API is a {@link ProblemDetail} with a stable {@code code} property
 * ({@code ai-rules/BACKEND.md} §3). Stack traces and upstream bodies never reach the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String CODE = "code";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UPSTREAM_UNAVAILABLE = "UPSTREAM_UNAVAILABLE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SandboxException.class)
    ProblemDetail handleSandbox(SandboxException failure) {
        SandboxErrorCode code = failure.code();
        if (code.isBusinessRule()) {
            log.info("Hotel rule refused the action: {}", code);
            return problem(HttpStatus.CONFLICT, code.name(), "The hotel system refused the action: " + code.name());
        }
        if (code == SandboxErrorCode.NOT_FOUND) {
            return problem(HttpStatus.NOT_FOUND, code.name(), "Not found in the hotel system");
        }
        if (code.isTransient()) {
            log.warn("Sandbox unavailable after retries: {}", code);
            return problem(HttpStatus.SERVICE_UNAVAILABLE, UPSTREAM_UNAVAILABLE,
                    "The hotel system is not responding. Please try again.");
        }
        // Our own bug or configuration: wrong PIN, unknown menu code, a malformed request.
        log.error("Sandbox rejected our request: {}", code, failure);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, "Something went wrong");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception failure) {
        log.error("Unhandled error", failure);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, "Something went wrong");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException failure,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail body = failure.getBody();
        body.setProperty(CODE, VALIDATION_ERROR);
        return handleExceptionInternal(failure, body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        return problem;
    }
}
