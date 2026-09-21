package com.example.parking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.parking.dto.ParkingResponse;
import com.example.parking.exception.ErrorCode;
import com.example.parking.service.ParkingService;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Commits real transactions, so it removes its sessions afterwards. */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestClockConfig.class})
class ParkingConcurrencyIT {

    @Autowired private ParkingService parkingService;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MutableClock clock;

    @AfterEach
    void removeSessions() {
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM parking_sessions");
    }

    @Test
    void simultaneousStartsForOneVehicleCreateOneSession() throws Exception {
        List<Object> outcomes = raceOnUserLock(
                () -> parkingService.start(1L, 1L, 1L),
                () -> parkingService.start(1L, 1L, 2L));

        assertThat(outcomes).filteredOn(ParkingResponse.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(ErrorCode.VEHICLE_ALREADY_PARKED::equals).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_sessions WHERE status = 'ACTIVE'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void simultaneousStopsKeepTheFirstResult() throws Exception {
        long id = parkingService.start(1L, 1L, 1L).id();
        clock.advance(Duration.ofMinutes(30));

        List<Object> outcomes = raceOnUserLock(
                () -> parkingService.stop(id, 1L),
                () -> parkingService.stop(id, 1L));

        ParkingResponse winner = outcomes.stream().filter(ParkingResponse.class::isInstance)
                .map(ParkingResponse.class::cast).findFirst().orElseThrow();
        assertThat(outcomes).filteredOn(ErrorCode.PARKING_ALREADY_COMPLETED::equals).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT amount FROM parking_sessions WHERE id = ?", String.class, id))
                .isEqualTo(winner.amount());
    }

    private List<Object> raceOnUserLock(Supplier<?>... actions) throws Exception {
        return UserLockRace.run(dataSource, jdbc, 1L, List.of(actions));
    }
}
