package com.example.parking.dto;

import com.example.parking.entity.ParkingZone;

public record ZoneResponse(Long id, String name, Long cityId, String pricePerHour, boolean active) {

    public static ZoneResponse from(ParkingZone zone) {
        return new ZoneResponse(zone.getId(), zone.getName(), zone.getCity().getId(),
                Money.format(zone.getPricePerHour()), zone.isActive());
    }
}
