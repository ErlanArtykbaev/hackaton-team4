package com.hackathon.team4.passport;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.PartyMember;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.safetylog.LogEntryType;
import com.hackathon.team4.safetylog.SafetyLogService;
import com.hackathon.team4.sandbox.generated.model.Guest;
import com.hackathon.team4.sandbox.generated.model.GuestPatch;
import com.hackathon.team4.sandbox.generated.model.Reservation;
import com.hackathon.team4.sandbox.pms.PmsGateway;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allergy passports: a card per family member in our database, the union of their allergens on the PMS profile.
 */
@Service
public class PassportService {

    private static final Logger log = LoggerFactory.getLogger(PassportService.class);

    /** Who changed the passport. */
    public enum Source {
        FRONT_DESK, WAITER
    }

    private final PassportRepository passports;
    private final PmsGateway pms;
    private final SafetyLogService safetyLog;
    private final Clock clock;

    PassportService(PassportRepository passports, PmsGateway pms, SafetyLogService safetyLog, Clock clock) {
        this.passports = passports;
        this.pms = pms;
        this.safetyLog = safetyLog;
        this.clock = clock;
    }

    /**
     * The passport with the PMS allergens next to it. Without a saved passport: the primary guest alone, with the
     * PMS allergens as {@code SEVERE} (the safe assumption until someone asks).
     */
    @Transactional(readOnly = true)
    public PassportSnapshot get(String reservationId) {
        Reservation reservation = pms.reservation(reservationId);
        return passports.findByReservationId(reservationId)
                .map(passport -> snapshot(passport, reservation, pmsAllergens(reservation)))
                .orElseGet(() -> defaultSnapshot(reservation));
    }

    /**
     * Who sits at the table, for the allergen engine. Reads the PMS only when there is no passport yet.
     */
    @Transactional(readOnly = true)
    public Party party(String reservationId) {
        Optional<Passport> passport = passports.findByReservationId(reservationId);
        if (passport.isPresent()) {
            Passport stored = passport.get();
            List<PartyMember> members = stored.members().stream()
                    .map(member -> new PartyMember(member.id(), member.name(), allergyMap(member)))
                    .toList();
            return new Party(reservationId, stored.roomNumber(), stored.guestName(), members);
        }
        PassportSnapshot snapshot = defaultSnapshot(pms.reservation(reservationId));
        List<PartyMember> members = snapshot.members().stream()
                .map(member -> new PartyMember(member.id(), member.name(), member.allergies()))
                .toList();
        return new Party(reservationId, snapshot.roomNumber(), snapshot.guestName(), members);
    }

    /**
     * Replaces the member cards, sets the PMS profile's allergies to their union and logs what changed. A member
     * without an id (or with an id we do not know yet) is new; a stored member missing from the list is removed.
     */
    @Transactional
    public PassportSnapshot save(String reservationId, String notes, List<PassportSnapshot.Member> members,
            String updatedBy, Source source, Integer rvc, String outletName) {
        Reservation reservation = pms.reservation(reservationId);
        Instant now = clock.instant();
        Optional<Passport> stored = passports.findByReservationId(reservationId);
        // the first save is compared with the default card the user saw
        String changes = describeChanges(stored
                .map(existing -> snapshot(existing, reservation, List.of()).members())
                .orElseGet(() -> defaultSnapshot(reservation).members()), members);
        Passport passport = stored.orElseGet(() -> new Passport(reservationId, now));

        passport.describe(reservation.getRoomNumber(), guestName(reservation.getGuest()), notes, updatedBy, now);
        List<PassportMember> kept = new ArrayList<>();
        for (PassportSnapshot.Member input : members) {
            PassportMember member = Optional.ofNullable(input.id())
                    .flatMap(passport::member)
                    .orElseGet(() -> input.id() == null ? passport.addMember(now) : passport.addMember(input.id(), now));
            member.update(input.name().trim(), input.age(), input.primary(), allergies(input.allergies()), now);
            kept.add(member);
        }
        passport.keepMembers(kept);
        Passport saved = passports.save(passport);

        Set<Allergen> union = union(members);
        syncPms(reservation.getGuest(), union);
        safetyLog.append(safetyLog.entry(LogEntryType.PASSPORT_UPDATED)
                .outlet(rvc, outletName)
                .reservation(reservationId, reservation.getRoomNumber(), guestName(reservation.getGuest()))
                .actor(updatedBy)
                .statement(source + ": " + changes)
                .build());
        log.info("Passport of reservation {} saved by {} ({}), {} members", reservationId, updatedBy, source,
                members.size());
        return snapshot(saved, reservation, List.copyOf(union));
    }

    private void syncPms(Guest guest, Set<Allergen> union) {
        Set<Allergen> current = guest.getAllergies() == null ? Set.of() : Set.copyOf(toOurs(guest.getAllergies()));
        if (current.equals(union)) {
            return;
        }
        // arrays replace the stored ones as a whole: send the other fields back unchanged
        GuestPatch patch = new GuestPatch()
                .allergies(union.stream()
                        .map(allergen -> com.hackathon.team4.sandbox.generated.model.Allergen.fromValue(allergen.name()))
                        .toList())
                .preferences(guest.getPreferences())
                .marketingConsent(guest.getMarketingConsent());
        pms.updateGuest(guest.getId(), patch);
    }

    private static PassportSnapshot snapshot(Passport passport, Reservation reservation,
            List<Allergen> pmsAllergens) {
        List<PassportSnapshot.Member> members = passport.members().stream()
                .map(member -> new PassportSnapshot.Member(member.id(), member.name(), member.age(), member.primary(),
                        allergyMap(member)))
                .toList();
        return new PassportSnapshot(passport.reservationId(), reservation.getRoomNumber(),
                guestName(reservation.getGuest()), passport.notes(), pmsAllergens, members,
                passport.updatedAt(), passport.updatedBy());
    }

    private static PassportSnapshot defaultSnapshot(Reservation reservation) {
        Map<Allergen, Severity> allergies = new EnumMap<>(Allergen.class);
        pmsAllergens(reservation).forEach(allergen -> allergies.put(allergen, Severity.SEVERE));
        String name = guestName(reservation.getGuest());
        PassportSnapshot.Member primary = new PassportSnapshot.Member(defaultMemberId(reservation.getId()), name,
                null, true, allergies);
        return new PassportSnapshot(reservation.getId(), reservation.getRoomNumber(), name, null,
                pmsAllergens(reservation), List.of(primary), null, null);
    }

    /** Stable, so the default primary guest keeps its id across reads and becomes the stored member on save. */
    static UUID defaultMemberId(String reservationId) {
        return UUID.nameUUIDFromBytes(("primary:" + reservationId).getBytes(StandardCharsets.UTF_8));
    }

    /** E.g. "added Anna; Anna: +MILK (intolerance); removed Oleg". */
    static String describeChanges(List<PassportSnapshot.Member> before, List<PassportSnapshot.Member> after) {
        Map<UUID, PassportSnapshot.Member> old = new HashMap<>();
        before.forEach(member -> old.put(member.id(), member));
        List<String> changes = new ArrayList<>();
        for (PassportSnapshot.Member member : after) {
            PassportSnapshot.Member previous = member.id() == null ? null : old.remove(member.id());
            Map<Allergen, Severity> was = previous == null ? Map.of() : previous.allergies();
            if (previous == null) {
                changes.add("added " + member.name().trim());
            }
            List<String> diff = new ArrayList<>();
            member.allergies().entrySet().stream()
                    .filter(entry -> was.get(entry.getKey()) != entry.getValue())
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> diff.add("+" + entry.getKey() + " (" + entry.getValue().name().toLowerCase()
                            + ")"));
            was.keySet().stream()
                    .filter(allergen -> !member.allergies().containsKey(allergen))
                    .sorted()
                    .forEach(allergen -> diff.add("-" + allergen));
            if (!diff.isEmpty()) {
                changes.add(member.name().trim() + ": " + String.join(", ", diff));
            }
        }
        old.values().forEach(removed -> changes.add("removed " + removed.name()));
        return changes.isEmpty() ? "no changes" : String.join("; ", changes);
    }

    private static Set<Allergen> union(List<PassportSnapshot.Member> members) {
        Set<Allergen> union = EnumSet.noneOf(Allergen.class);
        members.forEach(member -> union.addAll(member.allergies().keySet()));
        return union;
    }

    private static Set<MemberAllergy> allergies(Map<Allergen, Severity> allergies) {
        return allergies.entrySet().stream()
                .map(entry -> new MemberAllergy(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
    }

    private static Map<Allergen, Severity> allergyMap(PassportMember member) {
        Map<Allergen, Severity> map = new EnumMap<>(Allergen.class);
        member.allergies().forEach(allergy -> map.put(allergy.allergen(), allergy.severity()));
        return map;
    }

    private static List<Allergen> pmsAllergens(Reservation reservation) {
        Guest guest = reservation.getGuest();
        return guest == null || guest.getAllergies() == null ? List.of() : toOurs(guest.getAllergies());
    }

    private static List<Allergen> toOurs(List<com.hackathon.team4.sandbox.generated.model.Allergen> allergens) {
        return allergens.stream().filter(Objects::nonNull).map(allergen -> Allergen.valueOf(allergen.name())).toList();
    }

    private static String guestName(Guest guest) {
        if (guest == null) {
            return null;
        }
        return String.join(" ", Objects.toString(guest.getFirstName(), ""), Objects.toString(guest.getLastName(), ""))
                .trim();
    }
}
