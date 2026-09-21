package com.example.parking.controller;

import com.example.parking.dto.CityResponse;
import com.example.parking.dto.ZoneResponse;
import com.example.parking.service.CityService;
import com.example.parking.service.ParkingZoneService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;
    private final ParkingZoneService zoneService;

    public CityController(CityService cityService, ParkingZoneService zoneService) {
        this.cityService = cityService;
        this.zoneService = zoneService;
    }

    @GetMapping
    public List<CityResponse> listCities() {
        return cityService.listCities();
    }

    @GetMapping("/{cityId}/zones")
    public List<ZoneResponse> listZones(@PathVariable @Positive Long cityId) {
        return zoneService.listActiveZones(cityId);
    }
}
