package com.example.parking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.parking.service.UserService;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Commits real transactions, so it restores the seed balance afterwards. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabaseConfig.class)
class TopUpConcurrencyIT {

    @Autowired private UserService userService;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void restoreSeedBalance() {
        jdbc.update("UPDATE users SET balance = 20.00 WHERE id = 1");
    }

    @Test
    void concurrentTopUpsAreBothApplied() throws Exception {
        CompletableFuture<?> first;
        CompletableFuture<?> second;
        // Hold the user row so both top-ups queue on the lock, then release them together.
        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            blocker.createStatement().execute("SELECT id FROM users WHERE id = 1 FOR UPDATE");

            first = CompletableFuture.runAsync(() -> userService.topUp(1L, new BigDecimal("5.00")));
            second = CompletableFuture.runAsync(() -> userService.topUp(1L, new BigDecimal("7.00")));
            awaitLockWaiters(2);

            blocker.commit();
        }
        CompletableFuture.allOf(first, second).join();

        assertThat(jdbc.queryForObject("SELECT balance FROM users WHERE id = 1", BigDecimal.class))
                .isEqualByComparingTo("32.00");
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
        throw new AssertionError("Top-ups did not block on the user lock");
    }
}
