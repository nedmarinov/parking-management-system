package com.example.parking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.parking.service.ParkingService;
import com.example.parking.service.PaymentService;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Error mapping that the database-backed tests cannot reach through normal requests. */
@WebMvcTest(ParkingController.class)
class ErrorHandlingTest {

    private static final String START_BODY = "{\"userId\":1,\"vehicleId\":1,\"zoneId\":1}";

    @Autowired private MockMvc mvc;
    @MockitoBean private ParkingService parkingService;
    @MockitoBean private PaymentService paymentService;

    @Test
    void activeVehicleIndexViolationIsAlreadyParked() throws Exception {
        when(parkingService.start(any(), any(), any())).thenThrow(integrityViolation("uq_parking_sessions_active_vehicle"));

        expect(start(), 409, "VEHICLE_ALREADY_PARKED", "Vehicle already has an active parking");
    }

    @Test
    void paymentUniqueViolationIsAlreadyPaid() throws Exception {
        when(paymentService.pay(any(), any())).thenThrow(integrityViolation("uq_payments_parking_session"));

        expect(mvc.perform(post("/api/parkings/1/payment").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}")), 409, "PARKING_ALREADY_PAID", "Parking is already paid");
    }

    @Test
    void otherIntegrityViolationIsInternalError() throws Exception {
        when(parkingService.start(any(), any(), any())).thenThrow(integrityViolation("ck_users_balance_nonnegative"));

        expect(start(), 500, "INTERNAL_ERROR", "Unexpected server error");
    }

    @Test
    void unexpectedFailureHidesDetails() throws Exception {
        when(parkingService.start(any(), any(), any()))
                .thenThrow(new IllegalStateException("SELECT secret FROM users"));

        expect(start(), 500, "INTERNAL_ERROR", "Unexpected server error");
    }

    @Test
    void unknownRouteIsNotFound() throws Exception {
        expect(mvc.perform(get("/api/nope")), 404, "INVALID_REQUEST", "Invalid request");
    }

    @Test
    void unsupportedMethodIsRejected() throws Exception {
        expect(mvc.perform(delete("/api/parkings")), 405, "INVALID_REQUEST", "Invalid request");
    }

    @Test
    void nonJsonBodyIsRejected() throws Exception {
        expect(mvc.perform(post("/api/parkings").contentType(MediaType.TEXT_PLAIN).content(START_BODY)),
                415, "INVALID_REQUEST", "Invalid request");
    }

    private ResultActions start() throws Exception {
        return mvc.perform(post("/api/parkings").contentType(MediaType.APPLICATION_JSON).content(START_BODY));
    }

    private static DataIntegrityViolationException integrityViolation(String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
                new SQLException("violates constraint \"" + constraint + "\""));
    }

    private static void expect(ResultActions result, int status, String code, String message) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().json("{\"code\":\"%s\",\"message\":\"%s\"}".formatted(code, message),
                        JsonCompareMode.STRICT));
    }
}
