package com.hackathon.team4.order;

import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.Requirement;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.menu.MenuService;
import com.hackathon.team4.menu.dto.CondimentOption;
import com.hackathon.team4.menu.dto.MenuItem;
import com.hackathon.team4.order.dto.LineCheck;
import com.hackathon.team4.order.dto.OrderCheckResponse;
import com.hackathon.team4.order.dto.OrderLineRequest;
import com.hackathon.team4.order.dto.OrderRequest;
import com.hackathon.team4.order.dto.OrderResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Checks order lines against the party and sends orders to the POS.
 *
 * <p>Stub (BE-A2): lines are checked against the demo menu and nothing reaches the POS; BE-A4 checks them with the
 * allergen engine, enforces the requirements and opens a POS check linked to the reservation.
 */
@Service
public class OrderService {

    private static final int DEMO_CHECK_NUMBER = 1017;
    private static final long DEMO_CHECK_SEQ = 88231L;

    private final MenuService menus;

    public OrderService(MenuService menus) {
        this.menus = menus;
    }

    public OrderCheckResponse check(OrderRequest order) {
        return new OrderCheckResponse(checkLines(order));
    }

    public OrderResponse place(OrderRequest order) {
        return new OrderResponse(DEMO_CHECK_NUMBER, DEMO_CHECK_SEQ, checkLines(order));
    }

    private List<LineCheck> checkLines(OrderRequest order) {
        Map<String, MenuItem> menu = menus.menu(order.rvc(), order.reservationId()).stream()
                .collect(Collectors.toMap(MenuItem::code, Function.identity()));
        return order.lines().stream()
                .map(line -> checkLine(line, menu.get(line.code())))
                .toList();
    }

    private static LineCheck checkLine(OrderLineRequest line, MenuItem item) {
        if (item == null || item.verdict() == Verdict.UNKNOWN) {
            return new LineCheck(line.code(), Verdict.UNKNOWN, List.of(), Requirement.NONE);
        }
        List<DangerFor> dangerFor = Stream.concat(
                        item.dangerFor().stream(),
                        chosenOptions(item, line).flatMap(option -> option.dangerFor().stream()))
                .distinct()
                .toList();
        return new LineCheck(line.code(), verdictOf(dangerFor), dangerFor,
                requirementOf(dangerFor, line.forMemberId()));
    }

    private static Stream<CondimentOption> chosenOptions(MenuItem item, OrderLineRequest line) {
        return item.condimentGroups().stream()
                .flatMap(group -> group.options().stream())
                .filter(option -> line.condimentCodes().contains(option.code()));
    }

    private static Verdict verdictOf(List<DangerFor> dangerFor) {
        if (dangerFor.isEmpty()) {
            return Verdict.SAFE;
        }
        boolean severe = dangerFor.stream().anyMatch(danger -> danger.severity() == Severity.SEVERE);
        return severe ? Verdict.DANGER : Verdict.WARNING;
    }

    /** {@code PROJECT-CONTEXT.md} §3: consent for an allergic member or the table, otherwise the waiter confirms. */
    private static Requirement requirementOf(List<DangerFor> dangerFor, UUID forMemberId) {
        if (dangerFor.isEmpty()) {
            return Requirement.NONE;
        }
        boolean forAllergicMember = dangerFor.stream().anyMatch(danger -> danger.memberId().equals(forMemberId));
        if (forMemberId == null || forAllergicMember) {
            return Requirement.GUEST_CONSENT;
        }
        return Requirement.WAITER_CONFIRMATION;
    }
}
