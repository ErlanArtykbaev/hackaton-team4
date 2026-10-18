package com.hackathon.team4.order;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.AllergenEngine;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.PartyMember;
import com.hackathon.team4.allergen.Requirement;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.allergen.UnknownPartyMemberException;
import com.hackathon.team4.kitchen.KitchenTicketService;
import com.hackathon.team4.kitchen.KitchenTicketService.Round;
import com.hackathon.team4.kitchen.KitchenTicketService.RoundLine;
import com.hackathon.team4.menu.MenuService;
import com.hackathon.team4.menu.OutletService;
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
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogEntry;
import com.hackathon.team4.safetylog.SafetyLogService;
import com.hackathon.team4.sandbox.generated.model.BeginCheckResource;
import com.hackathon.team4.sandbox.generated.model.CheckDTO;
import com.hackathon.team4.sandbox.generated.model.CheckDetailDTO;
import com.hackathon.team4.sandbox.generated.model.CondimentItem;
import com.hackathon.team4.sandbox.generated.model.Detail;
import com.hackathon.team4.sandbox.generated.model.DictionaryElement;
import com.hackathon.team4.sandbox.pos.PosGateway;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Checks order lines against the party with the allergen engine and sends orders to the POS, the kitchen display and
 * the safety log.
 *
 * <p>An allergen never blocks a line: a line that needs the waiter's confirmation or the guest's consent goes out
 * once it has it ({@code PROJECT-CONTEXT.md} §3). Without a reservation (walk-in) nothing is matched.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final String LANGUAGE = "en";

    private final MenuService menus;
    private final OutletService outlets;
    private final PassportService passports;
    private final AllergenEngine engine;
    private final PosGateway pos;
    private final KitchenTicketService kitchen;
    private final SafetyLogService safetyLog;
    private final Clock clock;

    OrderService(MenuService menus, OutletService outlets, PassportService passports, AllergenEngine engine,
            PosGateway pos, KitchenTicketService kitchen, SafetyLogService safetyLog, Clock clock) {
        this.menus = menus;
        this.outlets = outlets;
        this.passports = passports;
        this.engine = engine;
        this.pos = pos;
        this.kitchen = kitchen;
        this.safetyLog = safetyLog;
        this.clock = clock;
    }

    public OrderCheckResponse check(OrderRequest order) {
        return new OrderCheckResponse(checkLines(order, party(order.reservationId())).stream()
                .map(CheckedLine::check)
                .toList());
    }

    /**
     * Once every line has the confirmation or consent it needs: opens a POS check linked to the reservation with the
     * lines as its first round, records the kitchen ticket with "for whom" and consents, and logs the order, each
     * waiter confirmation and each guest consent. Opening the check is never retried ({@link PosGateway#openCheck}).
     *
     * @throws OrderRejectedException when a line lacks what it needs, or names an item or member that does not exist
     */
    public OrderResponse place(OrderRequest order) {
        Party party = party(order.reservationId());
        List<CheckedLine> lines = checkLines(order, party);
        lines.forEach(OrderService::requireAnswered);

        CheckDTO check = pos.openCheck(order.rvc(), beginCheck(order, party, lines));
        int round = round(check);
        String outletName = outlets.outletName(order.rvc()).orElse(null);
        // A walk-in's round reaches the kitchen through the POS event feed, as a check without a reservation.
        if (party != null) {
            Round checked = new Round(order.rvc(), outletName, check.getCheckNumber(), check.getCheckSeq(), round,
                    order.table(), party.reservationId(), sentAt(check, round),
                    lines.stream().map(OrderService::roundLine).toList());
            try {
                kitchen.recordCheckedRound(checked);
            } catch (DataIntegrityViolationException raced) {
                // The POS event feed recorded the same round between the two steps; the retry replaces its ticket.
                kitchen.recordCheckedRound(checked);
            }
        }
        logOrder(order, party, outletName, check, lines);
        log.info("Order by {} sent to rvc {} as check {} round {} for reservation {}: {} lines", order.waiterCode(),
                order.rvc(), check.getCheckNumber(), round, order.reservationId(), lines.size());
        return new OrderResponse(Integer.parseInt(check.getCheckNumber()), Long.parseLong(check.getCheckSeq()),
                lines.stream().map(CheckedLine::check).toList());
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
            return new CheckedLine(number, line, item, condiments, allergens,
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

    private BeginCheckResource beginCheck(OrderRequest order, Party party, List<CheckedLine> lines) {
        return new BeginCheckResource()
                .orderType(orderType(order.rvc()))
                .table(order.table())
                .checkName(party == null ? null : party.roomNumber())
                .reservationId(party == null ? null : party.reservationId())
                .details(lines.stream().map(line -> new Detail()
                        .code(line.item().code())
                        .quantity(BigDecimal.valueOf(line.request().quantity()))
                        .condimentItems(line.condiments().stream()
                                .map(option -> new CondimentItem().code(option.code()))
                                .toList()))
                        .toList());
    }

    /** The outlet's first order type: dine in for a bar or a restaurant, room service for room service. */
    private String orderType(int rvc) {
        return pos.outlets(LANGUAGE).stream()
                .filter(outlet -> Objects.equals(outlet.getObjectNumber(), rvc) && outlet.getOrderTypes() != null)
                .flatMap(outlet -> outlet.getOrderTypes().stream())
                .map(DictionaryElement::getCode)
                .findFirst()
                .orElseThrow(() -> new OrderRejectedException(Reason.VALIDATION_ERROR,
                        "Outlet " + rvc + " takes no orders in the POS"));
    }

    /** The round our lines went out in, as the {@code pos.check.round_sent} feed names it. */
    private static int round(CheckDTO check) {
        return details(check).stream()
                .map(CheckDetailDTO::getRound)
                .filter(Objects::nonNull)
                .max(Integer::compare)
                .orElse(1);
    }

    private Instant sentAt(CheckDTO check, int round) {
        return details(check).stream()
                .filter(detail -> Objects.equals(detail.getRound(), round) && detail.getSentAt() != null)
                .map(detail -> detail.getSentAt().toInstant())
                .findFirst()
                .orElseGet(clock::instant);
    }

    private static List<CheckDetailDTO> details(CheckDTO check) {
        return check.getDetails() == null ? List.of() : check.getDetails();
    }

    private static RoundLine roundLine(CheckedLine line) {
        Requirement requirement = line.check().requirement();
        return new RoundLine(line.item().name(), line.request().quantity(),
                line.condiments().stream().map(CondimentOption::name).toList(), line.allergens(),
                line.request().forMemberId(), requirement == Requirement.WAITER_CONFIRMATION,
                requirement == Requirement.GUEST_CONSENT ? line.request().consent().givenBy() : null);
    }

    /** One entry for the order, one per waiter confirmation and one per guest consent, so the summary adds up. */
    private void logOrder(OrderRequest order, Party party, String outletName, CheckDTO check,
            List<CheckedLine> lines) {
        String table = order.table() == null ? "" : ", table " + order.table();
        safetyLog.append(entry(LogEntryType.ORDER_PLACED, order, party, outletName)
                .item(lines.stream()
                        .map(line -> (line.request().quantity() > 1 ? line.request().quantity() + " × " : "")
                                + label(line))
                        .collect(Collectors.joining(", ")))
                .statement("Check " + check.getCheckNumber() + table + ": " + lines.size()
                        + (lines.size() == 1 ? " line" : " lines") + " sent to the kitchen")
                .build());
        for (CheckedLine line : lines) {
            Requirement requirement = line.check().requirement();
            if (requirement == Requirement.WAITER_CONFIRMATION) {
                safetyLog.append(confirmation(order, party, outletName, line));
            } else if (requirement == Requirement.GUEST_CONSENT) {
                safetyLog.append(consent(order, party, outletName, line));
            }
        }
    }

    private SafetyLogEntry confirmation(OrderRequest order, Party party, String outletName, CheckedLine line) {
        List<DangerFor> dangers = line.check().dangerFor();
        DangerFor main = mostSevere(dangers);
        String forName = memberName(party, line.request().forMemberId());
        return entry(LogEntryType.WAITER_CONFIRMED, order, party, outletName)
                .member(forName)
                .item(label(line))
                .allergen(main.allergen(), main.severity())
                .statement(order.waiterCode() + " confirmed " + label(line) + " for " + forName + ": "
                        + dangers.stream().map(OrderService::describe).collect(Collectors.joining(", "))
                        + " at the table")
                .build();
    }

    /** For a member: their own allergens; for the table: everyone's. */
    private SafetyLogEntry consent(OrderRequest order, Party party, String outletName, CheckedLine line) {
        UUID forMemberId = line.request().forMemberId();
        List<DangerFor> dangers = line.check().dangerFor().stream()
                .filter(danger -> forMemberId == null || danger.memberId().equals(forMemberId))
                .toList();
        DangerFor main = mostSevere(dangers);
        String members = dangers.stream().map(DangerFor::memberName).distinct().collect(Collectors.joining(", "));
        String allergens = dangers.stream().map(danger -> danger.allergen().name()).distinct()
                .collect(Collectors.joining(", "));
        return entry(LogEntryType.GUEST_CONSENT, order, party, outletName)
                .member(members)
                .item(label(line))
                .allergen(main.allergen(), main.severity())
                .statement(line.request().consent().givenBy() + ": I know it contains " + allergens
                        + " and I order it")
                .build();
    }

    private SafetyLogEntry.Builder entry(LogEntryType type, OrderRequest order, Party party, String outletName) {
        SafetyLogEntry.Builder entry = safetyLog.entry(type).outlet(order.rvc(), outletName).actor(order.waiterCode());
        return party == null ? entry : entry.reservation(party.reservationId(), party.roomNumber(), party.guestName());
    }

    private static DangerFor mostSevere(List<DangerFor> dangers) {
        return dangers.stream().filter(danger -> danger.severity() == Severity.SEVERE).findFirst()
                .orElse(dangers.get(0));
    }

    private static String memberName(Party party, UUID memberId) {
        return party.members().stream()
                .filter(member -> member.id().equals(memberId))
                .map(PartyMember::name)
                .findFirst()
                .orElse(null);
    }

    /** The dish with its chosen condiments, e.g. "Espresso with Whole milk". */
    private static String label(CheckedLine line) {
        if (line.condiments().isEmpty()) {
            return line.item().name();
        }
        return line.item().name() + " with "
                + line.condiments().stream().map(CondimentOption::name).collect(Collectors.joining(", "));
    }

    private static String describe(DangerFor danger) {
        return danger.allergen() + " for " + danger.memberName() + " ("
                + danger.severity().name().toLowerCase(Locale.ROOT) + ")";
    }

    /** A line of the request with what the menu and the engine say about it. */
    private record CheckedLine(int number, OrderLineRequest request, MenuItem item, List<CondimentOption> condiments,
            Set<Allergen> allergens, LineCheck check) {
    }
}
