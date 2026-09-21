package com.example.parking.dto;

import com.example.parking.entity.City;

public record CityResponse(Long id, String name) {

    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getName());
    }
}
