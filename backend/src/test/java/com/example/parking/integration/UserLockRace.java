package com.example.parking.integration;

import com.example.parking.exception.ApiException;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Makes service calls overlap for real: holds a user's row lock until every action is queued on it,
 * then releases them together. Each outcome is the result, the ApiException's code, or the error.
 */
final class UserLockRace {

    private UserLockRace() {
    }

    static List<Object> run(DataSource dataSource, JdbcTemplate jdbc, long userId, List<Supplier<?>> actions)
            throws Exception {
        List<CompletableFuture<Object>> futures;
        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            blocker.createStatement().execute("SELECT id FROM users WHERE id = " + userId + " FOR UPDATE");
            futures = actions.stream().map(UserLockRace::outcomeOf).toList();
            awaitLockWaiters(jdbc, actions.size());
            blocker.commit();
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private static CompletableFuture<Object> outcomeOf(Supplier<?> action) {
        return CompletableFuture.supplyAsync(action::get).handle((result, error) -> {
            if (error instanceof CompletionException && error.getCause() instanceof ApiException api) {
                return api.getCode();
            }
            return error == null ? result : error;
        });
    }

    private static void awaitLockWaiters(JdbcTemplate jdbc, int expected) throws InterruptedException {
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
