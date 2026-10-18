package com.hackathon.team4;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import com.hackathon.team4.passport.PassportService;
import com.hackathon.team4.passport.PassportSnapshot;
import com.hackathon.team4.sandbox.SandboxStubs;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A family with a saved passport, for application tests that need a party at the table. The tests share one
 * database, so every caller passes its own reservation id and gets fresh member ids; the stubbed PMS answers with
 * that id, since the passport is stored under the id the PMS returns.
 */
public record TestParty(String reservationId, UUID mikhail, UUID anna, UUID misha) {

    /** Mikhail without allergies, Anna with a milk intolerance, Misha (7) with a severe nut allergy. */
    public static TestParty save(PassportService passports, WireMockServer sandbox, String reservationId) {
        String reservation = SandboxStubs.fixture("pms/reservation").replace("R-900641", reservationId);
        sandbox.stubFor(get(urlPathEqualTo("/v1/pms/reservations/" + reservationId))
                .willReturn(SandboxStubs.json(200, reservation)));
        sandbox.stubFor(patch(urlPathEqualTo("/v1/pms/guests/G-900641"))
                .willReturn(SandboxStubs.json(200, SandboxStubs.fixture("pms/guest-patched"))));
        TestParty party = new TestParty(reservationId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        passports.save(reservationId, null, List.of(
                        new PassportSnapshot.Member(party.mikhail(), "Mikhail", null, true, Map.of()),
                        new PassportSnapshot.Member(party.anna(), "Anna", null, false,
                                Map.of(Allergen.MILK, Severity.INTOLERANCE)),
                        new PassportSnapshot.Member(party.misha(), "Misha", 7, false,
                                Map.of(Allergen.NUTS, Severity.SEVERE))),
                "FD01", PassportService.Source.FRONT_DESK, null, null);
        return party;
    }
}
