package com.hackathon.team4.passport;

import com.hackathon.team4.sandbox.SandboxException;
import com.hackathon.team4.sandbox.events.SandboxEventReceived;
import com.hackathon.team4.sandbox.generated.model.Event;
import com.hackathon.team4.sandbox.generated.model.EventType;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the passport cards in step when someone changes a guest's allergies on the PMS profile directly.
 */
@Component
class GuestUpdatedListener {

    private static final Logger log = LoggerFactory.getLogger(GuestUpdatedListener.class);

    private final PassportService passports;

    GuestUpdatedListener(PassportService passports) {
        this.passports = passports;
    }

    @EventListener
    void onSandboxEvent(SandboxEventReceived received) {
        Event event = received.event();
        if (event.getType() != EventType.GUEST_UPDATED || !allergiesChanged(event)) {
            return;
        }
        if (!(event.getData().get("guestId") instanceof String guestId)) {
            log.warn("guest.updated event {} without a guestId", event.getKey());
            return;
        }
        try {
            passports.applyPmsAllergies(guestId);
        } catch (SandboxException failure) {
            // the next save of the passport sets the PMS to the union again; the cards stay as they are
            log.warn("PMS allergies of guest {} not applied to the passport ({})", guestId, failure.code());
        }
    }

    private static boolean allergiesChanged(Event event) {
        return event.getData() != null && event.getData().get("changedFields") instanceof Collection<?> fields
                && fields.contains("allergies");
    }
}
