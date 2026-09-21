package com.example.parking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseConfig.class)
@Transactional
class WalletApiIT {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    @Test
    void topUpIncreasesBalanceExactly() throws Exception {
        topUp(1, "{\"amount\":\"20\"}")
                .andExpect(status().isOk())
                .andExpect(content().json("{\"userId\":1,\"balance\":\"40.00\"}", JsonCompareMode.STRICT));
        assertThat(balance(1)).isEqualByComparingTo("40.00");
    }

    @Test
    void repeatedRequestAddsAgain() throws Exception {
        topUp(2, "{\"amount\":\"5.50\"}").andExpect(status().isOk());
        topUp(2, "{\"amount\":\"5.50\"}").andExpect(jsonPath("$.balance").value("21.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"amount\":null}", "{\"amount\":20}", "{\"amount\":\"\"}", "{\"amount\":\"0\"}",
            "{\"amount\":\"-1\"}", "{\"amount\":\"abc\"}", "{\"amount\":\"10.123\"}", "{\"amount\":\"1e2\"}",
            "{\"amount\":\"01.00\"}", "{\"amount\":\"1,00\"}", "{\"amount\":\"12345678901\"}"})
    void invalidAmountIsRejectedWithoutChange(String body) throws Exception {
        topUp(1, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOP_UP_AMOUNT"));
        assertThat(balance(1)).isEqualByComparingTo("20.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "not json", "[]", "\"20\""})
    void malformedBodyIsInvalidRequest(String body) throws Exception {
        topUp(1, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void invalidIdentifierTakesPrecedenceOverAmount() throws Exception {
        mvc.perform(post("/api/users/0/top-up").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        topUp(999, "{\"amount\":\"5\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void balanceLimitIsInclusive() throws Exception {
        jdbc.update("UPDATE users SET balance = 9999999999.00 WHERE id = 1");

        topUp(1, "{\"amount\":\"0.99\"}").andExpect(jsonPath("$.balance").value("9999999999.99"));
        topUp(1, "{\"amount\":\"0.01\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BALANCE_LIMIT_EXCEEDED"));
        assertThat(balance(1)).isEqualByComparingTo("9999999999.99");
    }

    private ResultActions topUp(long userId, String body) throws Exception {
        return mvc.perform(post("/api/users/" + userId + "/top-up")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private BigDecimal balance(long userId) {
        em.flush();
        return jdbc.queryForObject("SELECT balance FROM users WHERE id = ?", BigDecimal.class, userId);
    }
}
