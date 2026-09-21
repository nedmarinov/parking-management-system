package com.example.parking.service;

import com.example.parking.dto.ZoneResponse;
import com.example.parking.exception.ApiException;
import com.example.parking.exception.ErrorCode;
import com.example.parking.repository.CityRepository;
import com.example.parking.repository.ParkingZoneRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingZoneService {

    private final CityRepository cities;
    private final ParkingZoneRepository zones;

    public ParkingZoneService(CityRepository cities, ParkingZoneRepository zones) {
        this.cities = cities;
        this.zones = zones;
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> listActiveZones(Long cityId) {
        if (!cities.existsById(cityId)) {
            throw new ApiException(ErrorCode.CITY_NOT_FOUND, "City not found");
        }
        return zones.findByCityIdAndActiveTrueOrderByNameAscIdAsc(cityId).stream().map(ZoneResponse::from).toList();
    }
}
