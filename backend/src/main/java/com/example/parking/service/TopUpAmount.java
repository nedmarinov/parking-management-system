package com.example.parking.service;

import com.example.parking.exception.ApiException;
import com.example.parking.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** Parses the top-up amount exactly as the API contract defines it; never rounds. */
public final class TopUpAmount {

    private static final Pattern FORMAT = Pattern.compile("(0|[1-9][0-9]{0,9})(\\.[0-9]{1,2})?");
    private static final BigDecimal MIN = new BigDecimal("0.01");

    private TopUpAmount() {
    }

    public static BigDecimal parse(JsonNode amount) {
        if (amount == null || !amount.isTextual() || !FORMAT.matcher(amount.textValue()).matches()) {
            throw invalid();
        }
        BigDecimal value = new BigDecimal(amount.textValue()).setScale(2, RoundingMode.UNNECESSARY);
        if (value.compareTo(MIN) < 0) {
            throw invalid();
        }
        return value;
    }

    private static ApiException invalid() {
        return new ApiException(ErrorCode.INVALID_TOP_UP_AMOUNT,
                "Amount must be a decimal string from 0.01 to 9999999999.99 with at most two decimals");
    }
}
