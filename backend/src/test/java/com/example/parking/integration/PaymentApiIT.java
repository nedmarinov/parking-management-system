package com.example.parking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
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
@Import({TestDatabaseConfig.class, TestClockConfig.class})
@Transactional
class PaymentApiIT {

    private static final Instant START = Instant.parse("2026-09-21T10:15:00Z");

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(START);
    }

    @Test
    void completeFlowFromStartToPaidHistory() throws Exception {
        long id = startId(1, 1, 1);
        mvc.perform(get("/api/users/1/parkings/active")).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/users/1/parkings/history")).andExpect(content().json("[]", JsonCompareMode.STRICT));

        clock.advance(Duration.ofMinutes(65));
        act(id, "stop", 1).andExpect(status().isOk());
        mvc.perform(get("/api/users/1/parkings/history"))
                .andExpect(jsonPath("$[0].amount").value("4.00"))
                .andExpect(jsonPath("$[0].paymentStatus").value("UNPAID"));

        clock.advance(Duration.ofMinutes(2));
        act(id, "payment", 1)
                .andExpect(status().isCreated())
                .andExpect(content().json("""
                        {"parkingId":%d,"amount":"4.00","paymentStatus":"PAID",
                         "paidAt":"2026-09-21T11:22:00.000Z","remainingBalance":"16.00"}""".formatted(id),
                        JsonCompareMode.STRICT));

        mvc.perform(get("/api/users/1/parkings/history"))
                .andExpect(content().json("""
                        [{"id":%d,"userId":1,"vehicle":{"id":1,"plateNumber":"CA1234AB"},
                          "zone":{"id":1,"name":"Blue Zone","city":{"id":1,"name":"Sofia"}},
                          "hourlyRate":"2.00","startedAt":"2026-09-21T10:15:00.000Z",
                          "endedAt":"2026-09-21T11:20:00.000Z","amount":"4.00","status":"COMPLETED",
                          "paymentStatus":"PAID","paidAt":"2026-09-21T11:22:00.000Z"}]""".formatted(id),
                        JsonCompareMode.STRICT));
        assertThat(sql("SELECT balance FROM users WHERE id = 1")).isEqualTo("16.00");
        assertThat(sql("SELECT amount FROM payments WHERE parking_session_id = " + id)).isEqualTo("4.00");
    }

    @Test
    void insufficientFundsChangeNothingAndRetryAfterTopUpSucceeds() throws Exception {
        long id = completedSession(2, 3, 3, Duration.ofHours(7)); // 7 h at 1.50 = 10.50 > 10.00

        expectError(act(id, "payment", 2), 409, "INSUFFICIENT_BALANCE");
        assertThat(sql("SELECT balance FROM users WHERE id = 2")).isEqualTo("10.00");
        assertThat(sql("SELECT count(*) FROM payments")).isEqualTo("0");

        mvc.perform(post("/api/users/2/top-up").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"0.50\"}"))
                .andExpect(status().isOk());
        act(id, "payment", 2).andExpect(status().isCreated()).andExpect(jsonPath("$.remainingBalance").value("0.00"));
    }

    @Test
    void repeatedPaymentIsRejectedWithoutSecondDeduction() throws Exception {
        long id = completedSession(1, 1, 1, Duration.ofMinutes(30));
        act(id, "payment", 1).andExpect(status().isCreated());

        expectError(act(id, "payment", 1), 409, "PARKING_ALREADY_PAID");
        assertThat(sql("SELECT balance FROM users WHERE id = 1")).isEqualTo("18.00");
        assertThat(sql("SELECT count(*) FROM payments")).isEqualTo("1");
    }

    @Test
    void zeroAmountSessionIsPaidWithoutBalanceChange() throws Exception {
        Long zoneId = jdbc.queryForObject(
                "INSERT INTO parking_zones (city_id, name, price_per_hour) VALUES (1, 'Free', 0.00) RETURNING id",
                Long.class);
        long id = completedSession(1, 1, zoneId, Duration.ofHours(3));

        act(id, "payment", 1).andExpect(jsonPath("$.amount").value("0.00"))
                .andExpect(jsonPath("$.remainingBalance").value("20.00"));
    }

    @Test
    void paymentErrorsFollowContractOrder() throws Exception {
        long active = startId(1, 1, 1);
        long completed = completedSession(1, 2, 1, Duration.ofMinutes(10));

        expectError(act(completed, "payment", 999), 404, "USER_NOT_FOUND");
        expectError(act(999_999, "payment", 1), 404, "PARKING_NOT_FOUND");
        expectError(act(active, "payment", 2), 403, "PARKING_NOT_OWNED");
        expectError(act(active, "payment", 1), 400, "PARKING_NOT_COMPLETED");
        assertThat(sql("SELECT count(*) FROM payments")).isEqualTo("0");
    }

    @Test
    void paymentBeforeSessionEndIsRejected() throws Exception {
        long id = completedSession(1, 1, 1, Duration.ofMinutes(10));
        clock.set(START.plus(Duration.ofMinutes(10)).minusMillis(1));

        expectError(act(id, "payment", 1), 409, "CLOCK_BEFORE_SESSION_TIME");
        assertThat(sql("SELECT balance FROM users WHERE id = 1")).isEqualTo("20.00");
    }

    @Test
    void historyExcludesActiveAndOtherUsersAndIsNewestFirst() throws Exception {
        long older = completedSession(1, 1, 1, Duration.ofMinutes(5));
        long newer = completedSession(1, 2, 2, Duration.ofMinutes(5));
        act(older, "payment", 1).andExpect(status().isCreated()); // unpaid parking would block restarting vehicle 1
        startId(1, 1, 1);
        completedSession(2, 3, 3, Duration.ofMinutes(5));
        jdbc.update("UPDATE parking_zones SET price_per_hour = 9.00 WHERE id = 1");

        mvc.perform(get("/api/users/1/parkings/history"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newer))
                .andExpect(jsonPath("$[1].id").value(older))
                .andExpect(jsonPath("$[1].hourlyRate").value("2.00"))
                .andExpect(jsonPath("$[1].amount").value("2.00"));
    }

    @Test
    void historyForUnknownUserIsNotFound() throws Exception {
        expectError(mvc.perform(get("/api/users/999/parkings/history")), 404, "USER_NOT_FOUND");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"userId\":0}", "{\"userId\":\"1\"}"})
    void invalidPaymentBodyIsBadRequest(String body) throws Exception {
        long id = completedSession(1, 1, 1, Duration.ofMinutes(5));
        expectError(mvc.perform(post("/api/parkings/" + id + "/payment").contentType(MediaType.APPLICATION_JSON).content(body)),
                400, "INVALID_REQUEST");
    }

    /** Starts at the current clock, advances by {@code duration}, and stops. */
    private long completedSession(long userId, long vehicleId, long zoneId, Duration duration) throws Exception {
        long id = startId(userId, vehicleId, zoneId);
        clock.advance(duration);
        act(id, "stop", userId).andExpect(status().isOk());
        return id;
    }

    private long startId(long userId, long vehicleId, long zoneId) throws Exception {
        String body = mvc.perform(post("/api/parkings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d,\"vehicleId\":%d,\"zoneId\":%d}".formatted(userId, vehicleId, zoneId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions act(long parkingId, String action, long userId) throws Exception {
        return mvc.perform(post("/api/parkings/" + parkingId + "/" + action)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + userId + "}"));
    }

    private static void expectError(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
    }

    private String sql(String query) {
        em.flush();
        return jdbc.queryForObject(query, String.class);
    }
}
