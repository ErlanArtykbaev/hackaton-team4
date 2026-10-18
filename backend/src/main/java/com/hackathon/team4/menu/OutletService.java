package com.hackathon.team4.menu;

import com.hackathon.team4.menu.dto.Outlet;
import com.hackathon.team4.sandbox.SandboxException;
import com.hackathon.team4.sandbox.generated.model.OutletDictionaryElement;
import com.hackathon.team4.sandbox.pos.PosGateway;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The hotel's outlets from the POS, cached for 5 minutes like all reference data ({@code BACKEND.md} §5.2).
 */
@Service
public class OutletService {

    static final Duration TTL = Duration.ofMinutes(5);

    /** Staff screens are in English. */
    private static final String LANGUAGE = "en";

    private static final Logger log = LoggerFactory.getLogger(OutletService.class);

    private record Cached(List<Outlet> outlets, Instant fetchedAt) {
    }

    private final PosGateway pos;
    private final Clock clock;
    private volatile Cached cached;

    OutletService(PosGateway pos, Clock clock) {
        this.pos = pos;
        this.clock = clock;
    }

    public List<Outlet> outlets() {
        Instant now = clock.instant();
        Cached hit = cached;
        if (hit != null && hit.fetchedAt().plus(TTL).isAfter(now)) {
            return hit.outlets();
        }
        List<Outlet> outlets = pos.outlets(LANGUAGE).stream()
                .filter(outlet -> outlet.getObjectNumber() != null)
                .map(OutletService::toOutlet)
                .toList();
        cached = new Cached(outlets, now);
        return outlets;
    }

    /**
     * The outlet's name for logs and tickets; empty for an rvc the hotel does not have, or when the POS does not
     * answer, since a missing name must not stop a passport save or a kitchen ticket.
     */
    public Optional<String> outletName(int rvc) {
        try {
            return outlets().stream().filter(outlet -> outlet.rvc() == rvc).map(Outlet::name).findFirst();
        } catch (SandboxException failure) {
            log.warn("Outlet names unavailable ({}); rvc {} goes without one", failure.code(), rvc);
            return Optional.empty();
        }
    }

    private static Outlet toOutlet(OutletDictionaryElement outlet) {
        OutletKind kind = outlet.getKind() == null ? null : OutletKind.valueOf(outlet.getKind().name());
        List<String> tables = outlet.getTables() == null ? List.of() : List.copyOf(outlet.getTables());
        return new Outlet(outlet.getObjectNumber(), outlet.getName(), kind, tables);
    }
}
