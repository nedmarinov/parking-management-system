package com.example.parking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.parking.dto.ParkingResponse;
import com.example.parking.exception.ApiException;
import com.example.parking.exception.ErrorCode;
import com.example.parking.service.ParkingService;
import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Stream;
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

    /** Holds user 1's row lock until both actions are queued on it, then releases them together. */
    @SafeVarargs
    private List<Object> raceOnUserLock(Supplier<ParkingResponse>... actions) throws Exception {
        List<CompletableFuture<Object>> futures;
        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            blocker.createStatement().execute("SELECT id FROM users WHERE id = 1 FOR UPDATE");
            futures = Stream.of(actions).map(this::outcomeOf).toList();
            awaitLockWaiters(actions.length);
            blocker.commit();
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private CompletableFuture<Object> outcomeOf(Supplier<ParkingResponse> action) {
        return CompletableFuture.supplyAsync(action::get).handle((result, error) -> {
            if (error instanceof CompletionException && error.getCause() instanceof ApiException api) {
                return api.getCode();
            }
            return error == null ? result : error;
        });
    }

    private void awaitLockWaiters(int expected) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            Integer waiting = jdbc.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND wait_event_type = 'Lock'""", Integer.class);
            if (waiting != null && waiting >= expected) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Actions did not block on the user lock");
    }
}
