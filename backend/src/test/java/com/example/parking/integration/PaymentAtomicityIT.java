package com.example.parking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.example.parking.exception.ApiException;
import com.example.parking.repository.PaymentRepository;
import com.example.parking.service.ParkingService;
import com.example.parking.service.PaymentService;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Uses the real transaction manager and database; no outer test transaction can hide a rollback defect. */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestClockConfig.class})
class PaymentAtomicityIT {

    @Autowired private ParkingService parkingService;
    @Autowired private PaymentService paymentService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private MutableClock clock;
    @MockitoSpyBean private PaymentRepository payments;

    private long sessionId;

    @BeforeEach
    void completedSession() {
        sessionId = parkingService.start(1L, 1L, 1L).id();
        clock.advance(Duration.ofMinutes(30)); // 2.00
        parkingService.stop(sessionId, 1L);
    }

    @AfterEach
    void restoreSeedState() {
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM parking_sessions");
        jdbc.update("UPDATE users SET balance = 20.00 WHERE id = 1");
    }

    @Test
    void failureAfterFlushedDeductionRollsBackEverything() {
        AtomicReference<String> balanceInsideTransaction = new AtomicReference<>();
        doAnswer(invocation -> {
            em.flush();
            balanceInsideTransaction.set(balanceSql());
            throw new IllegalStateException("Simulated failure before payment commit");
        }).when(payments).save(any());

        assertThatThrownBy(() -> paymentService.pay(sessionId, 1L))
                .hasMessage("Simulated failure before payment commit");

        assertThat(balanceInsideTransaction.get()).as("deduction reached PostgreSQL").isEqualTo("18.00");
        assertThat(balanceSql()).isEqualTo("20.00");
        assertThat(paymentCount()).isZero();
    }

    @Test
    void successCommitsDeductionAndPayment() {
        paymentService.pay(sessionId, 1L);

        assertThat(balanceSql()).isEqualTo("18.00");
        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    void insufficientFundsCommitNeither() {
        jdbc.update("UPDATE users SET balance = 1.00 WHERE id = 1");

        assertThatThrownBy(() -> paymentService.pay(sessionId, 1L)).isInstanceOf(ApiException.class);

        assertThat(balanceSql()).isEqualTo("1.00");
        assertThat(paymentCount()).isZero();
    }

    private String balanceSql() {
        return jdbc.queryForObject("SELECT balance FROM users WHERE id = 1", String.class);
    }

    private int paymentCount() {
        return jdbc.queryForObject("SELECT count(*) FROM payments", Integer.class);
    }
}
