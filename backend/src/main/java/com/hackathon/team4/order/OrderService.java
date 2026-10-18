package com.hackathon.team4.order;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.AllergenEngine;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.PartyMember;
import com.hackathon.team4.allergen.Requirement;
import com.hackathon.team4.allergen.UnknownPartyMemberException;
import com.hackathon.team4.menu.MenuService;
import com.hackathon.team4.menu.dto.CondimentOption;
import com.hackathon.team4.menu.dto.MenuItem;
import com.hackathon.team4.order.OrderRejectedException.Reason;
import com.hackathon.team4.order.dto.LineCheck;
import com.hackathon.team4.order.dto.OrderCheckResponse;
import com.hackathon.team4.order.dto.OrderLineRequest;
import com.hackathon.team4.order.dto.OrderRequest;
import com.hackathon.team4.order.dto.OrderResponse;
import com.hackathon.team4.passport.Party;
import com.hackathon.team4.passport.PassportService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Checks order lines against the party with the allergen engine and sends orders to the POS.
 *
 * <p>An allergen never blocks a line: a line that needs the waiter's confirmation or the guest's consent goes out
 * once it has it ({@code PROJECT-CONTEXT.md} §3). Without a reservation (walk-in) nothing is matched.
 */
@Service
public class OrderService {

    private static final int DEMO_CHECK_NUMBER = 1017;
    private static final long DEMO_CHECK_SEQ = 88231L;

    private final MenuService menus;
    private final PassportService passports;
    private final AllergenEngine engine;

    OrderService(MenuService menus, PassportService passports, AllergenEngine engine) {
        this.menus = menus;
        this.passports = passports;
        this.engine = engine;
    }

    public OrderCheckResponse check(OrderRequest order) {
        return new OrderCheckResponse(checkLines(order, party(order.reservationId())).stream()
                .map(CheckedLine::check)
                .toList());
    }

    /**
     * Sends the order once every line has the confirmation or consent it needs. The POS check is opened in the next
     * step of BE-A4; until then the response carries a fixed check.
     *
     * @throws OrderRejectedException when a line lacks what it needs, or names an item or member that does not exist
     */
    public OrderResponse place(OrderRequest order) {
        List<CheckedLine> lines = checkLines(order, party(order.reservationId()));
        lines.forEach(OrderService::requireAnswered);
        return new OrderResponse(DEMO_CHECK_NUMBER, DEMO_CHECK_SEQ, lines.stream().map(CheckedLine::check).toList());
    }

    /** The party at the table, or {@code null} for a walk-in. */
    private Party party(String reservationId) {
        return reservationId == null || reservationId.isBlank() ? null : passports.party(reservationId);
    }

    private List<CheckedLine> checkLines(OrderRequest order, Party party) {
        Map<String, MenuItem> menu = menus.menu(order.rvc(), null).stream()
                .collect(Collectors.toMap(MenuItem::code, Function.identity(), (first, second) -> first));
        List<PartyMember> members = party == null ? List.of() : party.members();
        List<CheckedLine> lines = new ArrayList<>();
        for (OrderLineRequest line : order.lines()) {
            lines.add(checkLine(lines.size() + 1, line, menu, members));
        }
        return lines;
    }

    private CheckedLine checkLine(int number, OrderLineRequest line, Map<String, MenuItem> menu,
            List<PartyMember> members) {
        MenuItem item = menu.get(line.code());
        if (item == null) {
            throw new OrderRejectedException(Reason.VALIDATION_ERROR,
                    "Line " + number + ": item " + line.code() + " is not on this outlet's menu");
        }
        List<CondimentOption> condiments = condiments(number, item, line.condimentCodes());
        Set<Allergen> allergens = AllergenEngine.withCondiments(item.allergens(),
                condiments.stream().map(CondimentOption::allergens).toList());
        try {
            com.hackathon.team4.allergen.LineCheck result = engine.checkLine(members, allergens, line.forMemberId());
            return new CheckedLine(number, line, item,
                    new LineCheck(line.code(), result.verdict(), result.dangerFor(), result.requirement()));
        } catch (UnknownPartyMemberException unknown) {
            throw new OrderRejectedException(Reason.VALIDATION_ERROR,
                    "Line " + number + ": the guest it is for is not in the party");
        }
    }

    private static List<CondimentOption> condiments(int number, MenuItem item, List<String> codes) {
        Map<String, CondimentOption> options = item.condimentGroups().stream()
                .flatMap(group -> group.options().stream())
                .collect(Collectors.toMap(CondimentOption::code, Function.identity(), (first, second) -> first));
        return codes.stream().map(code -> {
            CondimentOption option = options.get(code);
            if (option == null) {
                throw new OrderRejectedException(Reason.VALIDATION_ERROR,
                        "Line " + number + ": condiment " + code + " does not belong to " + item.name());
            }
            return option;
        }).toList();
    }

    private static void requireAnswered(CheckedLine line) {
        Requirement requirement = line.check().requirement();
        String dangers = line.check().dangerFor().stream()
                .map(OrderService::describe)
                .collect(Collectors.joining(", "));
        String prefix = "Line " + line.number() + " (" + line.item().name() + ") needs ";
        if (requirement == Requirement.WAITER_CONFIRMATION && !line.request().waiterConfirmed()) {
            throw new OrderRejectedException(Reason.WAITER_CONFIRMATION_REQUIRED,
                    prefix + "the waiter's confirmation: " + dangers + " at the table");
        }
        if (requirement == Requirement.GUEST_CONSENT && line.request().consent() == null) {
            throw new OrderRejectedException(Reason.GUEST_CONSENT_REQUIRED,
                    prefix + "the guest's consent: " + dangers);
        }
    }

    private static String describe(DangerFor danger) {
        return danger.allergen() + " for " + danger.memberName() + " ("
                + danger.severity().name().toLowerCase(Locale.ROOT) + ")";
    }

    /** A line of the request with what the menu and the engine say about it. */
    private record CheckedLine(int number, OrderLineRequest request, MenuItem item, LineCheck check) {
    }
}
