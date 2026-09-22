package com.example.parking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabaseConfig.class)
@Transactional
class MigrationIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    @Test
    void migratesEmptyDatabaseAndSeedsDemoData() {
        assertThat(flyway.info().applied()).extracting(m -> m.getVersion().getVersion())
                .containsExactly("1", "2", "3");

        assertThat(jdbc.queryForList("SELECT name, balance FROM users ORDER BY id"))
                .extracting(r -> r.get("name"), r -> r.get("balance"))
                .containsExactly(
                        tuple("Alex Johnson", new BigDecimal("20.00")),
                        tuple("Maria Smith", new BigDecimal("10.00")));
        assertThat(jdbc.queryForList("SELECT plate_number FROM vehicles ORDER BY id", String.class))
                .containsExactly("CA1234AB", "CB5678CD", "PB1234CD");
        assertThat(jdbc.queryForList("SELECT name FROM cities ORDER BY id", String.class))
                .containsExactly("Sofia", "Plovdiv");
        assertThat(count("parking_zones")).isEqualTo(5);
        assertThat(count("parking_zones WHERE active")).isEqualTo(4);
        assertThat(count("parking_sessions")).isZero();
        assertThat(count("payments")).isZero();
    }

    @Test
    void rerunningMigrationsDoesNotReseed() {
        jdbc.update("UPDATE users SET balance = 55.00 WHERE id = 1");

        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT balance FROM users WHERE id = 1", BigDecimal.class))
                .isEqualByComparingTo("55.00");
    }

    @Test
    void generatedIdsFollowSeededIds() {
        assertThat(jdbc.queryForObject("INSERT INTO users (name) VALUES ('New') RETURNING id", Long.class))
                .isGreaterThan(2);
        assertThat(jdbc.queryForObject(
                "INSERT INTO vehicles (plate_number, user_id) VALUES ('NEW1', 1) RETURNING id", Long.class))
                .isGreaterThan(3);
        assertThat(jdbc.queryForObject("INSERT INTO cities (name) VALUES ('Varna') RETURNING id", Long.class))
                .isGreaterThan(2);
        assertThat(jdbc.queryForObject(
                "INSERT INTO parking_zones (city_id, name, price_per_hour) VALUES (1, 'New', 1.00) RETURNING id",
                Long.class))
                .isGreaterThan(5);
    }

    @Test
    void allowsOnlyOneActiveSessionPerVehicle() {
        insertCompletedSession(1, 1);
        insertActiveSession(1, 1);

        assertThatThrownBy(() -> insertActiveSession(1, 1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_parking_sessions_active_vehicle");
    }

    @Test
    void rejectsSessionForVehicleOwnedByAnotherUser() {
        assertThatThrownBy(() -> insertActiveSession(2, 1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_parking_sessions_vehicle_owner");
    }

    @Test
    void rejectsInconsistentSessionState() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO parking_sessions (user_id, vehicle_id, zone_id, hourly_rate, started_at, amount, status)
                VALUES (1, 1, 1, 2.00, now(), 2.00, 'ACTIVE')"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_parking_sessions_state");
    }

    @Test
    void allowsOnlyOnePaymentPerSession() {
        long sessionId = insertCompletedSession(1, 1);
        String insertPayment = """
                INSERT INTO payments (parking_session_id, amount, status, paid_at)
                VALUES (?, 2.00, 'PAID', now())""";
        jdbc.update(insertPayment, sessionId);

        assertThatThrownBy(() -> jdbc.update(insertPayment, sessionId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_payments_parking_session");
    }

    @Test
    void rejectsNegativeBalance() {
        assertThatThrownBy(() -> jdbc.update("UPDATE users SET balance = -0.01 WHERE id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_users_balance_nonnegative");
    }

    private long insertActiveSession(long userId, long vehicleId) {
        return jdbc.queryForObject("""
                INSERT INTO parking_sessions (user_id, vehicle_id, zone_id, hourly_rate, started_at, status)
                VALUES (?, ?, 1, 2.00, now(), 'ACTIVE') RETURNING id""", Long.class, userId, vehicleId);
    }

    private long insertCompletedSession(long userId, long vehicleId) {
        return jdbc.queryForObject("""
                INSERT INTO parking_sessions
                    (user_id, vehicle_id, zone_id, hourly_rate, started_at, ended_at, amount, status)
                VALUES (?, ?, 1, 2.00, now() - interval '1 hour', now(), 2.00, 'COMPLETED') RETURNING id""",
                Long.class, userId, vehicleId);
    }

    private long count(String tableAndFilter) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tableAndFilter, Long.class);
    }
}
