package com.hackathon.team4.menu;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.allergen.Verdict;
import com.hackathon.team4.menu.dto.CondimentGroup;
import com.hackathon.team4.menu.dto.CondimentOption;
import com.hackathon.team4.menu.dto.MenuItem;
import com.hackathon.team4.menu.dto.Outlet;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Outlets and their menus with a verdict per item for the party.
 *
 * <p>Stub (BE-A2): fixed data from the demo story; BE-A3 reads the outlets from the POS, BE-A4 the menu with
 * verdicts from the allergen engine.
 */
@Service
public class MenuService {

    private static final String DEMO_RESERVATION_ID = "R-203844";

    private static final DangerFor MISHA_PEANUTS = new DangerFor(
            UUID.fromString("518a0000-0000-4000-8000-000000000003"), "Misha", Allergen.PEANUTS, Severity.SEVERE);
    private static final DangerFor ANNA_MILK = new DangerFor(
            UUID.fromString("518a0000-0000-4000-8000-000000000002"), "Anna", Allergen.MILK, Severity.INTOLERANCE);

    private static final List<Outlet> OUTLETS = List.of(
            new Outlet(1, "Main Restaurant", OutletKind.RESTAURANT, List.of("T1", "T2", "T3", "T4", "T5", "T6")),
            new Outlet(2, "Beach Bar", OutletKind.BAR, List.of("B1", "B2", "B3", "B4", "B5", "B6")),
            new Outlet(3, "Room Service", OutletKind.ROOM_SERVICE, List.of()),
            new Outlet(4, "Pool Bar", OutletKind.BAR, List.of("P1", "P2", "P3", "P4", "P5", "P6")));

    private static final String THAI = "Thai classics";
    private static final String KIDS = "Kids";
    private static final String DESSERTS = "Desserts";
    private static final String DRINKS = "Drinks";

    /** Verdicts for the Kuznetsov party (room 518) once Anna's milk intolerance is on her card (demo step 2). */
    private static final List<MenuItem> DEMO_MENU = List.of(
            item("4101", "Chicken satay", THAI, 220, List.of(Allergen.PEANUTS),
                    Verdict.DANGER, List.of(MISHA_PEANUTS), List.of("4104")),
            item("4102", "Som tam", THAI, 180, List.of(Allergen.PEANUTS, Allergen.FISH),
                    Verdict.DANGER, List.of(MISHA_PEANUTS), List.of("4104")),
            item("4103", "Pad thai", THAI, 240, List.of(Allergen.PEANUTS, Allergen.EGGS, Allergen.FISH),
                    Verdict.DANGER, List.of(MISHA_PEANUTS), List.of("4104")),
            item("4104", "Spring rolls", THAI, 160, List.of(Allergen.GLUTEN, Allergen.SOYBEANS),
                    Verdict.SAFE, List.of(), List.of()),
            item("4201", "Kids nuggets", KIDS, 190, List.of(Allergen.GLUTEN, Allergen.EGGS),
                    Verdict.SAFE, List.of(), List.of()),
            item("4301", "Coconut ice cream", DESSERTS, 120, List.of(),
                    Verdict.SAFE, List.of(), List.of()),
            item("4401", "Thai iced tea", DRINKS, 110, List.of(Allergen.MILK),
                    Verdict.WARNING, List.of(ANNA_MILK), List.of("4402")),
            new MenuItem("4402", "Espresso", DRINKS, BigDecimal.valueOf(90), List.of(),
                    Verdict.SAFE, List.of(), List.of(),
                    List.of(new CondimentGroup("740", "Milk", 0, 1, List.of(
                            new CondimentOption("7401", "Whole milk", BigDecimal.ZERO, List.of(Allergen.MILK),
                                    Verdict.WARNING, List.of(ANNA_MILK)),
                            new CondimentOption("7402", "Oat milk", BigDecimal.valueOf(30), List.of(),
                                    Verdict.SAFE, List.of()))))));

    public List<Outlet> outlets() {
        return OUTLETS;
    }

    /** The outlet's name for logs; empty for an rvc the hotel does not have. */
    public Optional<String> outletName(int rvc) {
        return OUTLETS.stream().filter(outlet -> outlet.rvc() == rvc).map(Outlet::name).findFirst();
    }

    /**
     * The same menu for every outlet. The demo reservation gets its verdicts; a walk-in (no reservation) or any
     * other reservation gets {@code UNKNOWN}: allergens only, nothing matched.
     */
    public List<MenuItem> menu(int rvc, String reservationId) {
        if (DEMO_RESERVATION_ID.equals(reservationId)) {
            return DEMO_MENU;
        }
        return DEMO_MENU.stream().map(MenuService::walkInItem).toList();
    }

    private static MenuItem item(String code, String name, String group, int price, List<Allergen> allergens,
                                 Verdict verdict, List<DangerFor> dangerFor, List<String> safeAlternatives) {
        return new MenuItem(code, name, group, BigDecimal.valueOf(price), allergens, verdict, dangerFor,
                safeAlternatives, List.of());
    }

    private static MenuItem walkInItem(MenuItem item) {
        return new MenuItem(item.code(), item.name(), item.group(), item.price(), item.allergens(), Verdict.UNKNOWN,
                List.of(), List.of(), item.condimentGroups().stream().map(MenuService::walkInGroup).toList());
    }

    private static CondimentGroup walkInGroup(CondimentGroup group) {
        return new CondimentGroup(group.code(), group.name(), group.minSelect(), group.maxSelect(),
                group.options().stream().map(MenuService::walkInOption).toList());
    }

    private static CondimentOption walkInOption(CondimentOption option) {
        return new CondimentOption(option.code(), option.name(), option.price(), option.allergens(), Verdict.UNKNOWN,
                List.of());
    }
}
