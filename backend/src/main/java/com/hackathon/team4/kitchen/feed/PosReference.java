package com.hackathon.team4.kitchen.feed;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.sandbox.generated.model.CondimentGroupDTO;
import com.hackathon.team4.sandbox.generated.model.CondimentOptionDTO;
import com.hackathon.team4.sandbox.generated.model.MenuItemDTO;
import com.hackathon.team4.sandbox.pos.PosGateway;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Allergens of the menu items and condiments, which the kitchen feed needs for every round. Cached for 5 minutes
 * ({@code BACKEND.md} §5.2), so a busy POS does not cost a sandbox call per round.
 */
@Component
class PosReference {

    static final Duration TTL = Duration.ofMinutes(5);

    /** Staff screens are in English. */
    private static final String LANGUAGE = "en";

    /** Allergens by item code and by condiment code; a condiment code means the same option in every item. */
    record MenuAllergens(Map<String, Set<Allergen>> items, Map<String, Set<Allergen>> condiments) {

        static final MenuAllergens NONE = new MenuAllergens(Map.of(), Map.of());
    }

    private record Cached<T>(T value, Instant fetchedAt) {

        boolean fresh(Instant now) {
            return fetchedAt.plus(TTL).isAfter(now);
        }
    }

    private final PosGateway pos;
    private final Clock clock;
    private final Map<Integer, Cached<MenuAllergens>> menus = new ConcurrentHashMap<>();

    PosReference(PosGateway pos, Clock clock) {
        this.pos = pos;
        this.clock = clock;
    }

    MenuAllergens menu(int rvc) {
        return cached(menus, rvc, () -> allergens(pos.menu(rvc, LANGUAGE)));
    }


    private <K, T> T cached(Map<K, Cached<T>> cache, K key, Supplier<T> load) {
        Instant now = clock.instant();
        Cached<T> hit = cache.get(key);
        if (hit != null && hit.fresh(now)) {
            return hit.value();
        }
        T value = load.get();
        cache.put(key, new Cached<>(value, now));
        return value;
    }

    private static MenuAllergens allergens(List<MenuItemDTO> menu) {
        Map<String, Set<Allergen>> items = new HashMap<>();
        Map<String, Set<Allergen>> condiments = new HashMap<>();
        for (MenuItemDTO item : nullSafe(menu)) {
            items.put(item.getCode(), ours(item.getAllergens()));
            for (CondimentGroupDTO group : nullSafe(item.getCondiments())) {
                for (CondimentOptionDTO option : nullSafe(group.getItems())) {
                    condiments.put(option.getCode(), ours(option.getAllergens()));
                }
            }
        }
        return new MenuAllergens(Map.copyOf(items), Map.copyOf(condiments));
    }


    private static Set<Allergen> ours(List<com.hackathon.team4.sandbox.generated.model.Allergen> allergens) {
        Set<Allergen> ours = EnumSet.noneOf(Allergen.class);
        nullSafe(allergens).stream()
                .filter(Objects::nonNull)
                .forEach(allergen -> ours.add(Allergen.valueOf(allergen.name())));
        return Set.copyOf(ours);
    }

    private static <T> Collection<T> nullSafe(Collection<T> values) {
        return values == null ? List.of() : values;
    }
}
