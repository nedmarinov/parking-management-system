package com.example.parking.exception;

import com.example.parking.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return respond(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> handleInvalidRequest(Exception ex) {
        return respond(ErrorCode.INVALID_REQUEST, "Invalid request");
    }

    /** Database backstops for races the service checks missed; anything else stays a server error. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        String detail = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (detail.contains("uq_parking_sessions_active_vehicle")) {
            return respond(ErrorCode.VEHICLE_ALREADY_PARKED, "Vehicle already has an active parking");
        }
        if (detail.contains("uq_payments_parking_session")) {
            return respond(ErrorCode.PARKING_ALREADY_PAID, "Parking is already paid");
        }
        return handleUnexpected(ex);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Framework errors such as unknown routes or unsupported methods keep their 4xx status.
        if (ex instanceof org.springframework.web.ErrorResponse framework
                && framework.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(framework.getStatusCode())
                    .body(new ErrorResponse(ErrorCode.INVALID_REQUEST.name(), "Invalid request"));
        }
        log.error("Unexpected error", ex);
        return respond(ErrorCode.INTERNAL_ERROR, "Unexpected server error");
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.status()).body(new ErrorResponse(code.name(), message));
    }
}
