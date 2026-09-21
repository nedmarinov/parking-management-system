package com.example.parking.dto;

import com.example.parking.entity.Vehicle;

public record VehicleResponse(Long id, String plateNumber) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getPlateNumber());
    }
}
