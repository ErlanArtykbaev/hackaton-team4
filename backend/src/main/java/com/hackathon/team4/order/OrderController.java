package com.hackathon.team4.order;

import com.hackathon.team4.order.dto.OrderCheckResponse;
import com.hackathon.team4.order.dto.OrderRequest;
import com.hackathon.team4.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Orders from the waiter tablet: what each line needs, then sending the order to the POS.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    /** What each line needs (waiter confirmation, guest consent) before the order is sent. */
    @PostMapping("/check")
    public OrderCheckResponse check(@Valid @RequestBody OrderRequest order) {
        return orders.check(order);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "The check is open in the POS and the round is sent")
    @ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR, WAITER_CONFIRMATION_REQUIRED or GUEST_CONSENT_REQUIRED",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public OrderResponse place(@Valid @RequestBody OrderRequest order) {
        return orders.place(order);
    }
}
