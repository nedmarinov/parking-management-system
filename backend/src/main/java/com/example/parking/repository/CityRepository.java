package com.example.parking.repository;

import com.example.parking.entity.City;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findAllByOrderByNameAscIdAsc();
}
