package com.hackathon.team4.menu;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.AllergenEngine;
import com.hackathon.team4.allergen.ItemCheck;
import com.hackathon.team4.allergen.MenuCandidate;
import com.hackathon.team4.allergen.PartyMember;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.menu.dto.CondimentGroup;
import com.hackathon.team4.menu.dto.CondimentOption;
import com.hackathon.team4.menu.dto.MenuItem;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.sandbox.generated.model.CondimentGroupDTO;
import com.hackathon.team4.sandbox.generated.model.CondimentOptionDTO;
import com.hackathon.team4.sandbox.generated.model.MenuItemDTO;
import com.hackathon.team4.sandbox.pos.PosGateway;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The menu of an outlet with a verdict per item and per condiment for the party.
 *
 * <p>The menu comes from the POS, cached per outlet for {@link #MENU_TTL}; verdicts, "for whom" and safe
 * alternatives come from the allergen engine with the reservation's party.
 */
@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);

    /** Reference data is cached for 5 minutes ({@code BACKEND.md} §5.2). */
    static final Duration MENU_TTL = Duration.ofMinutes(5);

    /** Staff screens are in English. */
    private static final String LANGUAGE = "en";

    private final PosGateway pos;
    private final PassportService passports;
    private final AllergenEngine engine;
    private final Clock clock;
    private final Map<Integer, CachedMenu> cache = new ConcurrentHashMap<>();

    MenuService(PosGateway pos, PassportService passports, AllergenEngine engine, Clock clock) {
        this.pos = pos;
        this.passports = passports;
        this.engine = engine;
        this.clock = clock;
    }

    /**
     * The outlet's menu with verdicts for the reservation's party. Without a reservation (walk-in) every verdict is
     * {@code UNKNOWN}: allergens only, nothing matched.
     */
    public List<MenuItem> menu(int rvc, String reservationId) {
        List<MenuItem> items = posMenu(rvc);
        if (reservationId == null || reservationId.isBlank()) {
            return items;
        }
        List<PartyMember> party = passports.party(reservationId).members();
        List<MenuCandidate> candidates = items.stream().map(MenuService::candidate).toList();
        return items.stream().map(item -> withVerdicts(item, party, candidates)).toList();
    }

    /** The walk-in menu, read from the POS at most once per {@link #MENU_TTL}. */
    private List<MenuItem> posMenu(int rvc) {
        Instant now = clock.instant();
        CachedMenu cached = cache.get(rvc);
        if (cached != null && now.isBefore(cached.loadedAt().plus(MENU_TTL))) {
            return cached.items();
        }
        List<MenuItem> items = pos.menu(rvc, LANGUAGE).stream()
                .filter(item -> item.getType() != MenuItemDTO.TypeEnum.DISCOUNT)
                .map(MenuService::walkInItem)
                .toList();
        cache.put(rvc, new CachedMenu(now, items));
        log.info("Menu of rvc {} read from the POS: {} items", rvc, items.size());
        return items;
    }

    private MenuItem withVerdicts(MenuItem item, List<PartyMember> party, List<MenuCandidate> candidates) {
        ItemCheck check = engine.checkItem(party, item.allergens());
        return new MenuItem(item.code(), item.name(), item.group(), item.price(), item.allergens(), check.verdict(),
                check.dangerFor(), engine.safeAlternatives(candidates, party, candidate(item)),
                item.condimentGroups().stream().map(group -> withVerdicts(group, party)).toList());
    }

    private CondimentGroup withVerdicts(CondimentGroup group, List<PartyMember> party) {
        return new CondimentGroup(group.code(), group.name(), group.minSelect(), group.maxSelect(),
                group.options().stream().map(option -> {
                    ItemCheck check = engine.checkItem(party, option.allergens());
                    return new CondimentOption(option.code(), option.name(), option.price(), option.allergens(),
                            check.verdict(), check.dangerFor());
                }).toList());
    }

    private static MenuCandidate candidate(MenuItem item) {
        return new MenuCandidate(item.code(), item.group(), Set.copyOf(item.allergens()));
    }

    private static MenuItem walkInItem(MenuItemDTO item) {
        List<CondimentGroup> groups = orEmpty(item.getCondiments()).stream().map(MenuService::walkInGroup).toList();
        String group = item.getGroupL2() != null ? item.getGroupL2() : item.getGroupL1();
        return new MenuItem(item.getCode(), item.getName(), group, orZero(item.getPrice()),
                allergens(item.getAllergens()), Verdict.UNKNOWN, List.of(), List.of(), groups);
    }

    private static CondimentGroup walkInGroup(CondimentGroupDTO group) {
        List<CondimentOption> options = orEmpty(group.getItems()).stream().map(MenuService::walkInOption).toList();
        return new CondimentGroup(group.getCode(), group.getName(), Objects.requireNonNullElse(group.getMinSelect(), 0),
                Objects.requireNonNullElse(group.getMaxSelect(), options.size()), options);
    }

    private static CondimentOption walkInOption(CondimentOptionDTO option) {
        return new CondimentOption(option.getCode(), option.getName(), orZero(option.getPrice()),
                allergens(option.getAllergens()), Verdict.UNKNOWN, List.of());
    }

    private static List<Allergen> allergens(List<com.hackathon.team4.sandbox.generated.model.Allergen> allergens) {
        return orEmpty(allergens).stream()
                .filter(Objects::nonNull)
                .map(allergen -> Allergen.valueOf(allergen.name()))
                .toList();
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static BigDecimal orZero(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }

    private record CachedMenu(Instant loadedAt, List<MenuItem> items) {
    }
}
