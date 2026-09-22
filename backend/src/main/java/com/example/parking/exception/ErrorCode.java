package com.example.parking.exception;

import org.springframework.http.HttpStatus;

/** Machine-readable error codes from the API error contract. */
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    INVALID_TOP_UP_AMOUNT(HttpStatus.BAD_REQUEST),
    ZONE_INACTIVE(HttpStatus.BAD_REQUEST),
    PARKING_NOT_COMPLETED(HttpStatus.BAD_REQUEST),
    VEHICLE_NOT_OWNED(HttpStatus.FORBIDDEN),
    PARKING_NOT_OWNED(HttpStatus.FORBIDDEN),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND),
    CITY_NOT_FOUND(HttpStatus.NOT_FOUND),
    ZONE_NOT_FOUND(HttpStatus.NOT_FOUND),
    PARKING_NOT_FOUND(HttpStatus.NOT_FOUND),
    VEHICLE_ALREADY_PARKED(HttpStatus.CONFLICT),
    VEHICLE_HAS_UNPAID_PARKING(HttpStatus.CONFLICT),
    PARKING_ALREADY_COMPLETED(HttpStatus.CONFLICT),
    PARKING_ALREADY_PAID(HttpStatus.CONFLICT),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT),
    BALANCE_LIMIT_EXCEEDED(HttpStatus.CONFLICT),
    AMOUNT_LIMIT_EXCEEDED(HttpStatus.CONFLICT),
    CLOCK_BEFORE_SESSION_TIME(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
