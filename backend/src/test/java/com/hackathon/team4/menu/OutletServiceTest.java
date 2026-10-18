package com.hackathon.team4.menu;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.hackathon.team4.menu.dto.Outlet;
import com.hackathon.team4.sandbox.SandboxErrorCode;
import com.hackathon.team4.sandbox.SandboxException;
import com.hackathon.team4.sandbox.SandboxStubs;
import com.hackathon.team4.sandbox.SandboxTestBase;
import com.hackathon.team4.sandbox.pos.PosGateway;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A fresh service per test, so the cache of one test never hides another test's sandbox answer.
 */
class OutletServiceTest extends SandboxTestBase {

    @Autowired
    private PosGateway pos;

    @Autowired
    private Clock clock;

    private OutletService outlets;

    @BeforeEach
    void freshService() {
        outlets = new OutletService(pos, clock);
        stubs.posLogin();
    }

    @Test
    void shouldReadOutletsWithKindAndTablesFromThePos() {
        stubs.ok(WireMock.get(WireMock.urlPathEqualTo("/v1/pos/dict/outlets")), 200, "pos/outlets");

        List<Outlet> all = outlets.outlets();

        assertThat(all).extracting(Outlet::rvc).containsExactly(1, 2, 3);
        assertThat(all.get(0).kind()).isEqualTo(OutletKind.RESTAURANT);
        assertThat(all.get(0).tables()).contains("T1");
        assertThat(all.get(2).kind()).isEqualTo(OutletKind.ROOM_SERVICE);
        assertThat(outlets.outletName(2)).contains("Rooftop Wine Bar");
        assertThat(outlets.outletName(9)).isEmpty();
    }

    @Test
    void shouldAskThePosOnceWithinTheCacheTime() {
        stubs.ok(WireMock.get(WireMock.urlPathEqualTo("/v1/pos/dict/outlets")), 200, "pos/outlets");

        outlets.outlets();
        outlets.outletName(1);

        SANDBOX.verify(exactly(1), getRequestedFor(WireMock.urlPathEqualTo("/v1/pos/dict/outlets")));
    }

    @Test
    void shouldReportPosFailureForTheListButNotForAName() {
        stubs.failWith(WireMock.get(WireMock.urlPathEqualTo("/v1/pos/dict/outlets")), 503,
                SandboxStubs.UNAVAILABLE_BODY);

        assertThatThrownBy(() -> outlets.outlets())
                .isInstanceOf(SandboxException.class)
                .extracting("code").isEqualTo(SandboxErrorCode.SERVICE_UNAVAILABLE);
        assertThat(outlets.outletName(1)).isEmpty();
    }
}
