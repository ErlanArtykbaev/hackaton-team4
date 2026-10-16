package com.hackathon.team4.sandbox;

import com.hackathon.team4.sandbox.events.PlatformGateway;
import com.hackathon.team4.sandbox.pms.PmsGateway;
import com.hackathon.team4.sandbox.pms.ReservationQuery;
import com.hackathon.team4.sandbox.pos.CheckQuery;
import com.hackathon.team4.sandbox.pos.PosGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Read-only check of the live sandbox with the values in {@code backend/.env}. Skipped unless asked for:
 * {@code ./mvnw test -Dtest=LiveSandboxSmokeTest -Dsandbox.live=true}. Run it first thing with the new team key.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:team4-live",
        "sandbox.events.enabled=false"
})
@EnabledIfSystemProperty(named = "sandbox.live", matches = "true")
class LiveSandboxSmokeTest {

    @Autowired
    private PmsGateway pms;

    @Autowired
    private PosGateway pos;

    @Autowired
    private PlatformGateway platform;

    @Test
    void shouldReadLiveHotel() {
        var hotel = pms.hotel();
        assertThat(hotel.getBusinessDate()).isNotNull();
        assertThat(pms.rooms(null, null)).hasSize(hotel.getTotalRooms());
        assertThat(pms.reservations(ReservationQuery.inHouse())).isNotEmpty();

        var outlets = pos.outlets(null);
        assertThat(outlets).isNotEmpty();
        int rvc = outlets.getFirst().getObjectNumber();
        assertThat(pos.menu(rvc, null)).isNotEmpty();
        pos.checks(CheckQuery.openIn(rvc));

        assertThat(platform.events(0, 1, null).getSuccess()).isTrue();
    }
}
