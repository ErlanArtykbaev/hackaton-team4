package com.hackathon.team4.safetylog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@DataJpaTest
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({SafetyLogService.class, SafetyLogServiceTest.FixedClock.class})
class SafetyLogServiceTest {

    static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private SafetyLogService safetyLog;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void entries() {
        append(LogEntryType.ORDER_PLACED, "518", "Mikhail Kuznetsov", 4, NOW.minusSeconds(60));
        append(LogEntryType.GUEST_CONSENT, "518", "Mikhail Kuznetsov", 4, NOW.minusSeconds(50));
        append(LogEntryType.WAITER_CONFIRMED, "518", "Mikhail Kuznetsov", 4, NOW.minusSeconds(40));
        append(LogEntryType.POS_ORDER_UNCHECKED, "223", "Jane Doe", 1, NOW.minusSeconds(30));
        append(LogEntryType.ORDER_PLACED, "223", "Jane Doe", 1, NOW.minus(Duration.ofDays(2)));
    }

    @Test
    void shouldSearchByRoomNumberExactly() {
        assertThat(safetyLog.search(" 518 ", null, null)).hasSize(3);
        assertThat(safetyLog.search("51", null, null)).isEmpty();
    }

    @Test
    void shouldSearchByLastNameIgnoringCase() {
        assertThat(safetyLog.search("KUZN", null, null)).hasSize(3);
    }

    @Test
    void shouldCombineFiltersAndListNewestFirst() {
        assertThat(safetyLog.search("", null, null)).extracting(SafetyLogEntry::type).containsExactly(
                LogEntryType.POS_ORDER_UNCHECKED, LogEntryType.WAITER_CONFIRMED, LogEntryType.GUEST_CONSENT,
                LogEntryType.ORDER_PLACED, LogEntryType.ORDER_PLACED);
        assertThat(safetyLog.search(null, 4, LogEntryType.GUEST_CONSENT)).hasSize(1);
    }

    @Test
    void shouldPublishEveryAppendedEntry() {
        assertThat(events.stream(LogAppended.class)).hasSize(5).first()
                .satisfies(event -> assertThat(event.entry().roomNumber()).isEqualTo("518"));
    }

    @Test
    void shouldCountTheLastDayOnly() {
        assertThat(safetyLog.summary()).isEqualTo(new SafetyLogService.Counts(1, 2, 1, 1, 1));
    }

    private void append(LogEntryType type, String room, String guest, int rvc, Instant at) {
        safetyLog.append(SafetyLogEntry.of(type, at).outlet(rvc, null).reservation("R-" + room, room, guest)
                .actor("W02").build());
    }
}
