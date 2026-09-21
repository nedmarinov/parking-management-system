package com.example.parking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.parking.exception.ApiException;
import com.example.parking.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingServiceTest {

    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
    private final PricingService pricing = new PricingService();

    @ParameterizedTest(name = "{0} ms -> {1}")
    @CsvSource({
            "0,2.00", "1,2.00", "60000,2.00", "3540000,2.00", "3600000,2.00",
            "3600001,4.00", "3601000,4.00", "3660000,4.00", "7200000,4.00", "7260000,6.00"})
    void chargesEveryStartedHourWithOneHourMinimum(long elapsedMillis, String expected) {
        assertThat(pricing.calculate(new BigDecimal("2.00"), START, START.plusMillis(elapsedMillis)))
                .isEqualTo(new BigDecimal(expected));
    }

    @Test
    void zeroRateChargesZero() {
        assertThat(pricing.calculate(new BigDecimal("0.00"), START, START.plusSeconds(7200)))
                .isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void crossingMidnightUsesElapsedTime() {
        Instant start = Instant.parse("2026-01-01T23:30:00Z");
        assertThat(pricing.calculate(new BigDecimal("1.50"), start, Instant.parse("2026-01-02T00:31:00Z")))
                .isEqualTo(new BigDecimal("3.00"));
    }

    @Test
    void daylightSavingJumpUsesElapsedUtcTime() {
        // Sofia clocks jump 03:00 -> 04:00 on 2026-03-29: two hours on the wall clock, one elapsed hour.
        ZoneId sofia = ZoneId.of("Europe/Sofia");
        Instant start = LocalDateTime.parse("2026-03-29T02:30").atZone(sofia).toInstant();
        Instant end = LocalDateTime.parse("2026-03-29T04:30").atZone(sofia).toInstant();
        assertThat(pricing.calculate(new BigDecimal("2.00"), start, end)).isEqualTo(new BigDecimal("2.00"));
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> pricing.calculate(new BigDecimal("2.00"), START, START.minusMillis(1)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.CLOCK_BEFORE_SESSION_TIME));
    }
}
