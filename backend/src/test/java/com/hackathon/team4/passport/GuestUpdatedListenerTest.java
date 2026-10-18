package com.hackathon.team4.passport;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hackathon.team4.sandbox.SandboxErrorCode;
import com.hackathon.team4.sandbox.SandboxException;
import com.hackathon.team4.sandbox.events.SandboxEventReceived;
import com.hackathon.team4.sandbox.generated.model.Event;
import com.hackathon.team4.sandbox.generated.model.EventType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuestUpdatedListenerTest {

    private final PassportService passports = mock(PassportService.class);
    private final GuestUpdatedListener listener = new GuestUpdatedListener(passports);

    @Test
    void shouldApplyPmsAllergiesWhenTheyChanged() {
        listener.onSandboxEvent(event(EventType.GUEST_UPDATED, List.of("allergies", "preferences")));

        verify(passports).applyPmsAllergies("G-203844");
    }

    @Test
    void shouldIgnoreOtherFieldsAndOtherEvents() {
        listener.onSandboxEvent(event(EventType.GUEST_UPDATED, List.of("preferences")));
        listener.onSandboxEvent(event(EventType.POS_CHECK_ROUND_SENT, List.of("allergies")));

        verify(passports, never()).applyPmsAllergies(anyString());
    }

    @Test
    void shouldSurviveAPmsFailure() {
        doThrow(new SandboxException(SandboxErrorCode.SERVICE_UNAVAILABLE, 503, "down", null))
                .when(passports).applyPmsAllergies("G-203844");

        assertThatCode(() -> listener.onSandboxEvent(event(EventType.GUEST_UPDATED, List.of("allergies"))))
                .doesNotThrowAnyException();
    }

    private static SandboxEventReceived event(EventType type, List<String> changedFields) {
        return new SandboxEventReceived(new Event().key("k").type(type)
                .data(Map.of("guestId", "G-203844", "changedFields", changedFields)));
    }
}
