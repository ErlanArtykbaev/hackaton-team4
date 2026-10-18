package com.hackathon.team4.order.dto;

import java.util.List;

/**
 * The check of every line of an order, in the order of the request.
 */
public record OrderCheckResponse(List<LineCheck> lines) {
}
