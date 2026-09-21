package com.example.parking.service;

import com.example.parking.dto.CityResponse;
import com.example.parking.repository.CityRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CityService {

    private final CityRepository cities;

    public CityService(CityRepository cities) {
        this.cities = cities;
    }

    @Transactional(readOnly = true)
    public List<CityResponse> listCities() {
        return cities.findAllByOrderByNameAscIdAsc().stream().map(CityResponse::from).toList();
    }
}
