package com.example.parking.service;

import com.example.parking.exception.ApiException;
import com.example.parking.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** Hourly pricing: every started hour is charged, with a one-hour minimum. */
@Service
public class PricingService {

    private static final long HOUR_MILLIS = 3_600_000L;

    public BigDecimal calculate(BigDecimal hourlyRate, Instant startedAt, Instant endedAt) {
        long elapsed = Duration.between(startedAt, endedAt).toMillis();
        if (elapsed < 0) {
            throw new ApiException(ErrorCode.CLOCK_BEFORE_SESSION_TIME, "End time is before the session start");
        }
        long chargedHours = Math.max(1, elapsed / HOUR_MILLIS + (elapsed % HOUR_MILLIS > 0 ? 1 : 0));
        return hourlyRate.multiply(BigDecimal.valueOf(chargedHours));
    }
}
