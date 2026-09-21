package com.example.parking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StartParkingRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long vehicleId,
        @NotNull @Positive Long zoneId) {
}
