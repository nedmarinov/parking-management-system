package com.example.parking.repository;

import com.example.parking.entity.ParkingZone;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingZoneRepository extends JpaRepository<ParkingZone, Long> {

    List<ParkingZone> findByCityIdAndActiveTrueOrderByNameAscIdAsc(Long cityId);
}
