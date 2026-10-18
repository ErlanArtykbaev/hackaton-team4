package com.hackathon.team4.order.dto;

import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.Requirement;
import com.hackathon.team4.allergen.Verdict;
import java.util.List;

/**
 * What an order line means for the party, its chosen condiments included, and what it needs before sending.
 */
public record LineCheck(String code, Verdict verdict, List<DangerFor> dangerFor, Requirement requirement) {
}
