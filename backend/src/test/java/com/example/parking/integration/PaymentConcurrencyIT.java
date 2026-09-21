package com.example.parking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.parking.dto.BalanceResponse;
import com.example.parking.dto.PaymentResponse;
import com.example.parking.exception.ErrorCode;
import com.example.parking.service.ParkingService;
import com.example.parking.service.PaymentService;
import com.example.parking.service.UserService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Commits real transactions, so it restores seed balances and removes its sessions afterwards. */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestClockConfig.class})
class PaymentConcurrencyIT {

    @Autowired private ParkingService parkingService;
    @Autowired private PaymentService paymentService;
    @Autowired private UserService userService;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(Instant.parse("2026-01-01T10:00:00Z"));
    }

    @AfterEach
    void restoreSeedState() {
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM parking_sessions");
        jdbc.update("UPDATE users SET balance = CASE id WHEN 1 THEN 20.00 WHEN 2 THEN 10.00 END WHERE id IN (1, 2)");
    }

    @Test
    void twoPaymentsForOneSessionDeductOnce() throws Exception {
        long id = completedSession(1L, Duration.ofMinutes(30)); // 2.00

        List<Object> outcomes = race(() -> paymentService.pay(id, 1L), () -> paymentService.pay(id, 1L));

        assertThat(outcomes).filteredOn(PaymentResponse.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(ErrorCode.PARKING_ALREADY_PAID::equals).hasSize(1);
        assertThat(balance()).isEqualByComparingTo("18.00");
        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    void paymentsForTwoSessionsCannotOverdraw() throws Exception {
        long first = completedSession(1L, Duration.ofMinutes(30)); // 2.00
        long second = completedSession(2L, Duration.ofMinutes(30)); // 2.00
        jdbc.update("UPDATE users SET balance = 3.00 WHERE id = 1");

        List<Object> outcomes = race(() -> paymentService.pay(first, 1L), () -> paymentService.pay(second, 1L));

        assertThat(outcomes).filteredOn(PaymentResponse.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(ErrorCode.INSUFFICIENT_BALANCE::equals).hasSize(1);
        assertThat(balance()).isEqualByComparingTo("1.00");
        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    void topUpAndPaymentSerializeWithoutLostUpdate() throws Exception {
        long id = completedSession(1L, Duration.ofMinutes(61)); // 4.00
        jdbc.update("UPDATE users SET balance = 2.00 WHERE id = 1");

        List<Object> outcomes = race(
                () -> userService.topUp(1L, new BigDecimal("5.00")),
                () -> paymentService.pay(id, 1L));

        assertThat(outcomes.get(0)).isInstanceOf(BalanceResponse.class);
        if (outcomes.get(1) instanceof PaymentResponse) {
            // Top-up ran first: 2.00 + 5.00 - 4.00.
            assertThat(balance()).isEqualByComparingTo("3.00");
            assertThat(paymentCount()).isEqualTo(1);
        } else {
            // Payment ran first and was refused; the top-up still applies.
            assertThat(outcomes.get(1)).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
            assertThat(balance()).isEqualByComparingTo("7.00");
            assertThat(paymentCount()).isZero();
        }
    }

    private long completedSession(long vehicleId, Duration duration) {
        long id = parkingService.start(1L, vehicleId, 1L).id();
        clock.advance(duration);
        parkingService.stop(id, 1L);
        return id;
    }

    private List<Object> race(Supplier<?>... actions) throws Exception {
        return UserLockRace.run(dataSource, jdbc, 1L, List.of(actions));
    }

    private BigDecimal balance() {
        return jdbc.queryForObject("SELECT balance FROM users WHERE id = 1", BigDecimal.class);
    }

    private int paymentCount() {
        return jdbc.queryForObject("SELECT count(*) FROM payments", Integer.class);
    }
}
