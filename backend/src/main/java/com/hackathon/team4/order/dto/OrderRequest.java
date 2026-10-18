package com.hackathon.team4.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * An order from the waiter tablet: the same body checks the lines and sends them to the POS.
 * Without {@code reservationId} (walk-in) nothing is matched.
 */
public record OrderRequest(
        @Positive int rvc,
        String reservationId,
        String table,
        @NotBlank String waiterCode,
        @NotEmpty @Valid List<OrderLineRequest> lines) {
}
