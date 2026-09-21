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
class ParkingApiIT {

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
    void startReturnsActiveSessionWithCapturedRateAndServerTime() throws Exception {
        long id = startId(1, 1, 1);

        mvc.perform(get("/api/users/1/parkings/active"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":%d,"userId":1,"vehicle":{"id":1,"plateNumber":"CA1234AB"},
                          "zone":{"id":1,"name":"Blue Zone","city":{"id":1,"name":"Sofia"}},
                          "hourlyRate":"2.00","startedAt":"2026-09-21T10:15:00.000Z","endedAt":null,
                          "amount":null,"status":"ACTIVE","paymentStatus":null,"paidAt":null}]""".formatted(id),
                        JsonCompareMode.STRICT));
    }

    @Test
    void stopCompletesUnpaidWithFinalChargeAndNoBalanceChange() throws Exception {
        long id = startId(1, 1, 1);
        clock.advance(Duration.ofMinutes(65));

        stop(id, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endedAt").value("2026-09-21T11:20:00.000Z"))
                .andExpect(jsonPath("$.amount").value("4.00"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.paidAt").isEmpty());
        mvc.perform(get("/api/users/1/parkings/active")).andExpect(content().json("[]"));
        assertThat(sql("SELECT balance FROM users WHERE id = 1")).isEqualTo("20.00");
        assertThat(sql("SELECT count(*) FROM payments")).isEqualTo("0");
    }

    @Test
    void repeatedStopKeepsFirstResult() throws Exception {
        long id = startId(1, 1, 1);
        clock.advance(Duration.ofMinutes(30));
        stop(id, 1).andExpect(status().isOk());
        clock.advance(Duration.ofHours(5));

        stop(id, 1).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PARKING_ALREADY_COMPLETED"));
        assertThat(sql("SELECT amount FROM parking_sessions WHERE id = " + id)).isEqualTo("2.00");
    }

    @Test
    void capturedRateSurvivesCatalogChange() throws Exception {
        long id = startId(1, 1, 1);
        jdbc.update("UPDATE parking_zones SET price_per_hour = 3.00 WHERE id = 1");
        clock.advance(Duration.ofMinutes(90));

        stop(id, 1).andExpect(jsonPath("$.hourlyRate").value("2.00")).andExpect(jsonPath("$.amount").value("4.00"));
    }

    @Test
    void stopStillWorksAfterZoneBecomesInactive() throws Exception {
        long id = startId(1, 1, 1);
        jdbc.update("UPDATE parking_zones SET active = false WHERE id = 1");

        stop(id, 1).andExpect(status().isOk());
    }

    @Test
    void differentVehiclesCanParkAndListNewestFirst() throws Exception {
        long first = startId(1, 1, 1);
        clock.advance(Duration.ofMinutes(1));
        long second = startId(1, 2, 3);
        startId(2, 3, 2);

        mvc.perform(get("/api/users/1/parkings/active"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[1].id").value(first));
    }

    @Test
    void sameVehicleCannotParkTwiceButCanAfterCompletion() throws Exception {
        long id = startId(1, 1, 1);
        start(1, 1, 2).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VEHICLE_ALREADY_PARKED"));

        stop(id, 1).andExpect(status().isOk());
        start(1, 1, 2).andExpect(status().isCreated());
    }

    @Test
    void startErrorsInsertNothing() throws Exception {
        expectError(start(999, 1, 1), 404, "USER_NOT_FOUND");
        expectError(start(1, 999, 1), 404, "VEHICLE_NOT_FOUND");
        expectError(start(2, 1, 1), 403, "VEHICLE_NOT_OWNED");
        expectError(start(1, 1, 999), 404, "ZONE_NOT_FOUND");
        expectError(start(2, 3, 5), 400, "ZONE_INACTIVE");
        assertThat(sql("SELECT count(*) FROM parking_sessions")).isEqualTo("0");
    }

    @Test
    void stopErrorsCheckUserThenSessionThenOwnership() throws Exception {
        long id = startId(1, 1, 1);

        expectError(stop(id, 999), 404, "USER_NOT_FOUND");
        expectError(stop(999_999, 1), 404, "PARKING_NOT_FOUND");
        expectError(stop(id, 2), 403, "PARKING_NOT_OWNED");
        assertThat(sql("SELECT status FROM parking_sessions WHERE id = " + id)).isEqualTo("ACTIVE");
    }

    @Test
    void clockBeforeStartRejectsStopAndKeepsSessionActive() throws Exception {
        long id = startId(1, 1, 1);
        clock.set(START.minusMillis(1));

        expectError(stop(id, 1), 409, "CLOCK_BEFORE_SESSION_TIME");
        assertThat(sql("SELECT status FROM parking_sessions WHERE id = " + id)).isEqualTo("ACTIVE");
    }

    @Test
    void unrepresentableChargeRejectsStopAndKeepsSessionActive() throws Exception {
        Long zoneId = jdbc.queryForObject(
                "INSERT INTO parking_zones (city_id, name, price_per_hour) VALUES (1, 'Max', 9999999999.99) RETURNING id",
                Long.class);
        long id = startId(1, 1, zoneId);
        clock.advance(Duration.ofMinutes(61));

        expectError(stop(id, 1), 409, "AMOUNT_LIMIT_EXCEEDED");
        assertThat(sql("SELECT status FROM parking_sessions WHERE id = " + id)).isEqualTo("ACTIVE");
    }

    @Test
    void activeListForUnknownUserIsNotFound() throws Exception {
        expectError(mvc.perform(get("/api/users/999/parkings/active")), 404, "USER_NOT_FOUND");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"userId\":1,\"vehicleId\":1}", "{\"userId\":0,\"vehicleId\":1,\"zoneId\":1}",
            "{\"userId\":\"abc\",\"vehicleId\":1,\"zoneId\":1}", "{\"userId\":1.5,\"vehicleId\":1,\"zoneId\":1}",
            "{\"userId\":\"1\",\"vehicleId\":1,\"zoneId\":1}", "{\"userId\":null,\"vehicleId\":1,\"zoneId\":1}"})
    void invalidStartBodyIsBadRequest(String body) throws Exception {
        expectError(mvc.perform(post("/api/parkings").contentType(MediaType.APPLICATION_JSON).content(body)),
                400, "INVALID_REQUEST");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"userId\":-1}", "{\"userId\":\"x\"}"})
    void invalidStopBodyIsBadRequest(String body) throws Exception {
        long id = startId(1, 1, 1);
        expectError(mvc.perform(post("/api/parkings/" + id + "/stop").contentType(MediaType.APPLICATION_JSON)
                .content(body)), 400, "INVALID_REQUEST");
    }

    private ResultActions start(long userId, long vehicleId, long zoneId) throws Exception {
        return mvc.perform(post("/api/parkings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":%d,\"vehicleId\":%d,\"zoneId\":%d}".formatted(userId, vehicleId, zoneId)));
    }

    private long startId(long userId, long vehicleId, long zoneId) throws Exception {
        String body = start(userId, vehicleId, zoneId).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions stop(long parkingId, long userId) throws Exception {
        return mvc.perform(post("/api/parkings/" + parkingId + "/stop").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + userId + "}"));
    }

    private static void expectError(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
    }

    private String sql(String query) {
        em.flush();
        return jdbc.queryForObject(query, String.class);
    }
}
