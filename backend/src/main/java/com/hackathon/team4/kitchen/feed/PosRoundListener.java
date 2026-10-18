package com.hackathon.team4.kitchen.feed;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.kitchen.KitchenTicketService;
import com.hackathon.team4.kitchen.KitchenTicketService.Round;
import com.hackathon.team4.kitchen.KitchenTicketService.RoundLine;
import com.hackathon.team4.sandbox.SandboxException;
import com.hackathon.team4.sandbox.events.SandboxEventReceived;
import com.hackathon.team4.sandbox.generated.model.Event;
import com.hackathon.team4.sandbox.generated.model.EventType;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns every {@code pos.check.round_sent} into a kitchen ticket, rounds typed straight into the POS included.
 * {@code checkSeq} and {@code round} are kept exactly as in the event key
 * ({@code pos.check.round_sent:rvc:checkNumber:checkSeq:round}), the form the tablet uses for its own rounds, so
 * a round is recorded once.
 */
@Component
class PosRoundListener {

    private static final Logger log = LoggerFactory.getLogger(PosRoundListener.class);

    private static final Set<String> ALLERGEN_NAMES =
            Arrays.stream(Allergen.values()).map(Enum::name).collect(Collectors.toUnmodifiableSet());

    private final KitchenTicketService tickets;
    private final PosReference reference;
    private final Clock clock;

    PosRoundListener(KitchenTicketService tickets, PosReference reference, Clock clock) {
        this.tickets = tickets;
        this.reference = reference;
        this.clock = clock;
    }

    @EventListener
    void onSandboxEvent(SandboxEventReceived received) {
        Event event = received.event();
        if (event.getType() != EventType.POS_CHECK_ROUND_SENT) {
            return;
        }
        Round round;
        try {
            round = round(event);
        } catch (IllegalArgumentException malformed) {
            log.warn("pos.check.round_sent event {} skipped: {}", event.getKey(), malformed.getMessage());
            return;
        }
        try {
            tickets.recordPosRound(round);
        } catch (RuntimeException failure) {
            // a listener must not throw: the other listeners of this event would not get it
            log.error("Kitchen ticket for {} not recorded", event.getKey(), failure);
        }
    }

    private Round round(Event event) {
        Map<String, Object> data = event.getData();
        if (data == null) {
            throw new IllegalArgumentException("no data");
        }
        int rvc = number(data, "rvc");
        PosReference.MenuAllergens menu = menu(rvc, event);
        List<RoundLine> lines = list(data.get("items")).stream()
                .map(item -> line(asMap(item), menu))
                .toList();
        Instant sentAt = event.getOccurredAt() == null ? clock.instant() : event.getOccurredAt().toInstant();
        return new Round(rvc, outletName(rvc), required(data, "checkNumber"), required(data, "checkSeq"),
                number(data, "round"), text(data.get("table")), text(data.get("reservationId")), sentAt, lines);
    }

    /**
     * The event's own item allergens plus what the menu says for the item and its condiments: a missing allergen is
     * the dangerous case, so the union wins.
     */
    private static RoundLine line(Map<?, ?> item, PosReference.MenuAllergens menu) {
        Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);
        allergens.addAll(allergens(item.get("allergens")));
        allergens.addAll(menu.items().getOrDefault(text(item.get("code")), Set.of()));
        List<String> condiments = new ArrayList<>();
        for (Object condiment : list(item.get("condiments"))) {
            String code = condiment instanceof Map<?, ?> fields ? text(fields.get("code")) : text(condiment);
            String name = condiment instanceof Map<?, ?> fields ? text(fields.get("name")) : null;
            condiments.add(name == null ? code : name);
            allergens.addAll(menu.condiments().getOrDefault(code, Set.of()));
        }
        String name = text(item.get("name"));
        if (name == null) {
            throw new IllegalArgumentException("item without a name");
        }
        Object quantity = item.get("quantity");
        return RoundLine.fromPos(name, quantity instanceof Number number ? number.intValue() : 1, condiments,
                allergens);
    }

    /** Without the menu (POS unavailable) the ticket still shows the allergens the event carries. */
    private PosReference.MenuAllergens menu(int rvc, Event event) {
        try {
            return reference.menu(rvc);
        } catch (SandboxException failure) {
            log.warn("Menu of rvc {} unavailable ({}); ticket for {} shows the event's allergens only", rvc,
                    failure.code(), event.getKey());
            return PosReference.MenuAllergens.NONE;
        }
    }

    private String outletName(int rvc) {
        try {
            return reference.outletName(rvc).orElse(null);
        } catch (SandboxException failure) {
            log.warn("Outlet names unavailable ({}); ticket of rvc {} without one", failure.code(), rvc);
            return null;
        }
    }

    private static Set<Allergen> allergens(Object values) {
        return list(values).stream()
                .map(PosRoundListener::text)
                .filter(ALLERGEN_NAMES::contains)
                .map(Allergen::valueOf)
                .collect(Collectors.toSet());
    }

    private static int number(Map<String, Object> data, String field) {
        if (!(data.get(field) instanceof Number number)) {
            throw new IllegalArgumentException("no " + field);
        }
        return number.intValue();
    }

    /** {@code checkNumber} and {@code checkSeq} come as strings; a number is written the same way. */
    private static String required(Map<String, Object> data, String field) {
        String value = text(data.get(field));
        if (value == null) {
            throw new IllegalArgumentException("no " + field);
        }
        return value;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> values ? values : List.of();
    }

    private static Map<?, ?> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("item is not an object");
        }
        return map;
    }
}
