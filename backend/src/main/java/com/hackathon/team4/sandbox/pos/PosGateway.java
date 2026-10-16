package com.hackathon.team4.sandbox.pos;

import com.hackathon.team4.config.SandboxProperties;
import com.hackathon.team4.sandbox.Pages;
import com.hackathon.team4.sandbox.PosTokenProvider;
import com.hackathon.team4.sandbox.SandboxCalls;
import com.hackathon.team4.sandbox.SandboxErrorCode;
import com.hackathon.team4.sandbox.SandboxException;
import com.hackathon.team4.sandbox.generated.api.PosApi;
import com.hackathon.team4.sandbox.generated.model.BeginCheckResource;
import com.hackathon.team4.sandbox.generated.model.CheckDTO;
import com.hackathon.team4.sandbox.generated.model.CheckListResponse;
import com.hackathon.team4.sandbox.generated.model.MenuItemDTO;
import com.hackathon.team4.sandbox.generated.model.OutletDictionaryElement;
import com.hackathon.team4.sandbox.generated.model.PmsReservationInfo;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

import static com.hackathon.team4.sandbox.Pages.MAX_SIZE;

/**
 * The mini-POS: outlets, menus, guest lookup, checks. Logs in again once when the token is rejected.
 * <p>
 * Opening a check and sending a round are never retried automatically: a repeat would open a second check or send
 * the dishes to the kitchen twice. After such a failure, re-read the open checks and decide.
 */
@Component
public class PosGateway {

    private final PosApi api;
    private final SandboxCalls calls;
    private final PosTokenProvider tokens;
    private final SandboxProperties properties;

    PosGateway(PosApi api, SandboxCalls calls, PosTokenProvider tokens, SandboxProperties properties) {
        this.api = api;
        this.calls = calls;
        this.tokens = tokens;
        this.properties = properties;
    }

    /** Revenue centers with tables, order types and tenders. {@code language}: {@code en} or the hotel's language. */
    public List<OutletDictionaryElement> outlets(String language) {
        return pos(true, () -> api.listOutlets(language).getElements());
    }

    public List<MenuItemDTO> menu(int rvc, String language) {
        return pos(true, () -> api.listMenu(rvc, language).getElements());
    }

    /** In-house guests by exact room number or last-name prefix (2+ characters). */
    public List<PmsReservationInfo> lookup(int rvc, String roomOrLastName) {
        return pos(true, () -> api.pmsLookup(rvc, roomOrLastName).getElements());
    }

    public List<CheckDTO> checks(CheckQuery q) {
        return Pages.all(
                page -> pos(true, () -> api.listChecks(q.rvc(), q.status(), q.from(), q.to(), q.reservationId(),
                        page, MAX_SIZE)),
                CheckListResponse::getElements,
                CheckListResponse::getPageMetadata);
    }

    public CheckDTO check(int checkNumber, int rvc, long checkSeq) {
        return pos(true, () -> api.getCheck(checkNumber, rvc, checkSeq).getCheck());
    }

    /** Opens a check, optionally with a first round and a payment. Never retried. */
    public CheckDTO openCheck(int rvc, BeginCheckResource body) {
        return pos(false, () -> api.createCheck(rvc, properties.deviceId(), body).getCheck());
    }

    /**
     * Adds a round and/or takes a payment. Retried only for a pure payment that carries a {@code postingGuid}.
     */
    public CheckDTO updateCheck(int checkNumber, int rvc, long checkSeq, BeginCheckResource body) {
        boolean sendsRound = body.getDetails() != null && !body.getDetails().isEmpty();
        boolean repeatable = !sendsRound && body.getPostingGuid() != null;
        return pos(repeatable, () -> api.updateCheck(checkNumber, rvc, checkSeq, body).getCheck());
    }

    private <T> T pos(boolean repeatable, Supplier<T> call) {
        try {
            return calls.call(repeatable, call);
        } catch (SandboxException failure) {
            if (failure.code() != SandboxErrorCode.UNAUTHORIZED) {
                throw failure;
            }
            // Rejected before processing, so repeating once with a fresh token is safe for any call.
            tokens.invalidate();
            return calls.call(repeatable, call);
        }
    }
}
