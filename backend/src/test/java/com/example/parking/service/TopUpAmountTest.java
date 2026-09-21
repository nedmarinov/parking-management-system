package com.example.parking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.parking.exception.ApiException;
import com.example.parking.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TopUpAmountTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({"20,20.00", "20.0,20.00", "20.00,20.00", "5.50,5.50", "0.01,0.01",
            "9999999999.99,9999999999.99"})
    void acceptsContractFormatsAndNormalizesToTwoDecimals(String input, String expected) throws Exception {
        assertThat(TopUpAmount.parse(JSON.readTree("\"" + input + "\"")).toPlainString()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"\"", "\"0\"", "\"0.00\"", "\"-1\"", "\"abc\"", "\"10.123\"", "\"1e2\"",
            "\"01.00\"", "\"1,00\"", "\"12345678901\"", "\" 1\"", "\"1 \"", "\"€1\"", "\"+1\"", "\"1.\"", "\".5\"",
            "20", "20.00", "null", "true", "{}", "[]"})
    void rejectsEverythingElse(String json) throws Exception {
        JsonNode node = JSON.readTree(json);
        assertThatThrownBy(() -> TopUpAmount.parse(node))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_TOP_UP_AMOUNT));
    }

    @Test
    void rejectsMissingAmount() {
        assertThatThrownBy(() -> TopUpAmount.parse(null)).isInstanceOf(ApiException.class);
    }
}
