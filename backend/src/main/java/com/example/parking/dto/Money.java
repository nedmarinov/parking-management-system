package com.example.parking.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formats stored money as the API's two-decimal string. */
public final class Money {

    /** Largest value that fits numeric(12,2). */
    public static final BigDecimal MAX = new BigDecimal("9999999999.99");

    private Money() {
    }

    public static String format(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
