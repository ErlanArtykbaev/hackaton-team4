package com.hackathon.team4.sandbox;

/**
 * A failed sandbox call. Features branch on {@link #code()}, never on the message.
 */
public class SandboxException extends RuntimeException {

    private final SandboxErrorCode code;
    private final int httpStatus;

    public SandboxException(SandboxErrorCode code, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public SandboxErrorCode code() {
        return code;
    }

    /** HTTP status of the sandbox response, or 0 when there was no response. */
    public int httpStatus() {
        return httpStatus;
    }
}
