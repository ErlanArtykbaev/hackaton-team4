package com.hackathon.team4.order;

import com.hackathon.team4.common.error.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * An order refused before anything reached the POS: 400 with the reason as the API's {@code code}.
 */
public class OrderRejectedException extends ErrorResponseException {

    public enum Reason {
        /** An item, condiment or "for whom" member the order names does not exist. */
        VALIDATION_ERROR,
        WAITER_CONFIRMATION_REQUIRED,
        GUEST_CONSENT_REQUIRED
    }

    private final Reason reason;

    public OrderRejectedException(Reason reason, String detail) {
        super(HttpStatus.BAD_REQUEST, problem(reason, detail), null);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    private static ProblemDetail problem(Reason reason, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setProperty(GlobalExceptionHandler.CODE, reason.name());
        return problem;
    }
}
