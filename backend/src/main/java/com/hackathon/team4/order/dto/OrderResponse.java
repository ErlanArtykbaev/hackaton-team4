package com.hackathon.team4.order.dto;

import java.util.List;

/**
 * An order sent to the POS: the check it opened and the check of every line.
 */
public record OrderResponse(int checkNumber, long checkSeq, List<LineCheck> lines) {
}
