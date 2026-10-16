package com.hackathon.team4.sandbox;

import com.hackathon.team4.sandbox.generated.model.BeginCheckResource;
import com.hackathon.team4.sandbox.generated.model.Detail;
import com.hackathon.team4.sandbox.generated.model.FolioPostingRequest;
import com.hackathon.team4.sandbox.generated.model.Hotel;
import com.hackathon.team4.sandbox.generated.model.Reservation;
import com.hackathon.team4.sandbox.pms.PmsGateway;
import com.hackathon.team4.sandbox.pms.ReservationQuery;
import com.hackathon.team4.sandbox.pos.PosGateway;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.hackathon.team4.sandbox.SandboxStubs.RATE_LIMITED_BODY;
import static com.hackathon.team4.sandbox.SandboxStubs.UNAVAILABLE_BODY;
import static com.hackathon.team4.sandbox.SandboxStubs.fixture;
import static com.hackathon.team4.sandbox.SandboxStubs.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The cross-cutting failure scenarios of {@code ai-rules/TESTING.md} §2.
 */
class SandboxResilienceTest extends SandboxTestBase {

    private static final String RES_ID = "R-900641";

    @Autowired
    private PmsGateway pms;

    @Autowired
    private PosGateway pos;

    @Test
    void shouldRetryWhenRateLimitedThenSucceed() {
        stubs.failOnceThenOk(() -> get(urlPathEqualTo("/v1/pms/hotel")), 429, RATE_LIMITED_BODY, 200, "pms/hotel");

        Hotel hotel = pms.hotel();

        assertThat(hotel.getBusinessDate()).isNotNull();
        SANDBOX.verify(exactly(2), getRequestedFor(urlPathEqualTo("/v1/pms/hotel")));
    }

    @Test
    void shouldFailWithServiceUnavailableAfterThreeAttempts() {
        stubs.failWith(get(urlPathEqualTo("/v1/pms/rooms")), 503, UNAVAILABLE_BODY);

        assertThatThrownBy(() -> pms.rooms(null, null))
                .isInstanceOfSatisfying(SandboxException.class, failure -> {
                    assertThat(failure.code()).isEqualTo(SandboxErrorCode.SERVICE_UNAVAILABLE);
                    assertThat(failure.httpStatus()).isEqualTo(503);
                });
        SANDBOX.verify(exactly(3), getRequestedFor(urlPathEqualTo("/v1/pms/rooms")));
    }

    @Test
    void shouldLogInAgainOnceWhenPosTokenRejected() {
        stubs.posLogin()
                .failOnceThenOk(() -> get(urlPathEqualTo("/v1/pos/dict/outlets")),
                        401, fixture("errors/401-pos-unauthorized"), 200, "pos/outlets");

        assertThat(pos.outlets(null)).hasSize(3);

        SANDBOX.verify(exactly(2), postRequestedFor(urlPathEqualTo("/v1/auth/token")));
        SANDBOX.verify(exactly(2), getRequestedFor(urlPathEqualTo("/v1/pos/dict/outlets")));
    }

    @Test
    void shouldSendApiKeyToPmsAndBearerTokenToPos() {
        stubs.ok(get(urlPathEqualTo("/v1/pms/hotel")), 200, "pms/hotel")
                .posLogin()
                .ok(get(urlPathEqualTo("/v1/pos/dict/outlets")), 200, "pos/outlets");

        pms.hotel();
        pos.outlets(null);

        SANDBOX.verify(getRequestedFor(urlPathEqualTo("/v1/pms/hotel")).withHeader("X-API-KEY", equalTo("test-key")));
        SANDBOX.verify(postRequestedFor(urlPathEqualTo("/v1/auth/token"))
                .withHeader("X-API-KEY", equalTo("test-key"))
                .withRequestBody(matchingJsonPath("$.accessCode", equalTo("TEST-CODE"))));
        SANDBOX.verify(getRequestedFor(urlPathEqualTo("/v1/pos/dict/outlets"))
                .withHeader("Authorization", equalTo("Bearer test-token")));
    }

    @Test
    void shouldNotRetryOpeningCheckWhenSandboxUnavailable() {
        stubs.posLogin().failWith(post(urlPathEqualTo("/v1/pos/checks")), 503, UNAVAILABLE_BODY);
        BeginCheckResource body = new BeginCheckResource().orderType("3").checkName("205")
                .addDetailsItem(new Detail().code("2011").quantity(BigDecimal.ONE));

        assertThatThrownBy(() -> pos.openCheck(3, body))
                .isInstanceOfSatisfying(SandboxException.class,
                        failure -> assertThat(failure.code()).isEqualTo(SandboxErrorCode.SERVICE_UNAVAILABLE));
        SANDBOX.verify(exactly(1), postRequestedFor(urlPathEqualTo("/v1/pos/checks"))
                .withHeader("X-Device-Id", equalTo("test-device")));
    }

    @Test
    void shouldNotRetrySendingRoundWhenSandboxUnavailable() {
        stubs.posLogin().failWith(put(urlPathMatching("/v1/pos/checks/1099")), 503, UNAVAILABLE_BODY);
        BeginCheckResource round = new BeginCheckResource()
                .addDetailsItem(new Detail().code("2011").quantity(BigDecimal.ONE));

        assertThatThrownBy(() -> pos.updateCheck(1099, 3, 83334L, round)).isInstanceOf(SandboxException.class);
        SANDBOX.verify(exactly(1), putRequestedFor(urlPathEqualTo("/v1/pos/checks/1099")));
    }

    @Test
    void shouldRetryRoomChargeWithSamePostingGuid() {
        String guid = UUID.randomUUID().toString();
        stubs.posLogin().failOnceThenOk(() -> put(urlPathEqualTo("/v1/pos/checks/1099")), 503,
                UNAVAILABLE_BODY, 200, "pos/check-paid");
        BeginCheckResource payment = new BeginCheckResource().tender("10").tenderAmount(new BigDecimal("17.00"))
                .paymentAccount(RES_ID).postingGuid(UUID.fromString(guid));

        pos.updateCheck(1099, 3, 83334L, payment);

        SANDBOX.verify(exactly(2), putRequestedFor(urlPathEqualTo("/v1/pos/checks/1099"))
                .withRequestBody(matchingJsonPath("$.postingGuid", equalTo(guid))));
    }

    @Test
    void shouldReuseSamePostingGuidWhenRetryingFolioPosting() {
        String path = "/v1/pms/reservations/" + RES_ID + "/folio";
        stubs.failOnceThenOk(() -> post(urlPathEqualTo(path)), 503, UNAVAILABLE_BODY, 201, "pms/folio-posted");
        UUID guid = UUID.randomUUID();
        FolioPostingRequest posting = new FolioPostingRequest()
                .description("Late checkout 16:00").amount(new BigDecimal("40.00")).postingGuid(guid);

        pms.postToFolio(RES_ID, posting);

        SANDBOX.verify(exactly(2), postRequestedFor(urlPathEqualTo(path))
                .withRequestBody(matchingJsonPath("$.postingGuid", equalTo(guid.toString()))));
    }

    @Test
    void shouldNotRetryAndKeepCodeWhenHotelRuleRefuses() {
        String path = "/v1/pms/reservations/R-900001/folio";
        stubs.failWith(post(urlPathEqualTo(path)), 400, fixture("errors/400-guest-not-in-house"));
        FolioPostingRequest posting = new FolioPostingRequest()
                .description("Minibar").amount(BigDecimal.TEN).postingGuid(UUID.randomUUID());

        assertThatThrownBy(() -> pms.postToFolio("R-900001", posting))
                .isInstanceOfSatisfying(SandboxException.class, failure -> {
                    assertThat(failure.code()).isEqualTo(SandboxErrorCode.GUEST_NOT_IN_HOUSE);
                    assertThat(failure.code().isBusinessRule()).isTrue();
                });
        SANDBOX.verify(exactly(1), postRequestedFor(urlPathEqualTo(path)));
    }

    @Test
    void shouldReadEveryPageOfReservations() {
        SANDBOX.stubFor(get(urlPathEqualTo("/v1/pms/reservations")).withQueryParam("page", equalTo("0"))
                .willReturn(json(200, page("R-1", 0))));
        SANDBOX.stubFor(get(urlPathEqualTo("/v1/pms/reservations")).withQueryParam("page", equalTo("1"))
                .willReturn(json(200, page("R-2", 1))));

        List<Reservation> all = pms.reservations(ReservationQuery.inHouse());

        assertThat(all).extracting(Reservation::getId).containsExactly("R-1", "R-2");
        SANDBOX.verify(getRequestedFor(urlPathEqualTo("/v1/pms/reservations"))
                .withQueryParam("status", equalTo("IN_HOUSE")).withQueryParam("size", equalTo("200")));
    }

    private static String page(String reservationId, int number) {
        return """
                {"success": true, "message": null,
                 "elements": [{"id": "%s", "status": "IN_HOUSE"}],
                 "pageMetadata": {"number": %d, "size": 1, "totalElements": 2, "totalPages": 2}}
                """.formatted(reservationId, number);
    }
}
