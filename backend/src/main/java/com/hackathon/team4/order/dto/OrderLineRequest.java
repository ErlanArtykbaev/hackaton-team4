package com.hackathon.team4.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

/**
 * One line of an order. {@code forMemberId} is the party member it is for, {@code null} for the table;
 * {@code waiterConfirmed} and {@code consent} answer what the line requires.
 */
public record OrderLineRequest(
        @NotBlank String code,
        @Positive int quantity,
        @NotNull List<String> condimentCodes,
        UUID forMemberId,
        boolean waiterConfirmed,
        @Valid ConsentRequest consent) {
}
