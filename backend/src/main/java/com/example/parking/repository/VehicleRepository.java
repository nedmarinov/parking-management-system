package com.example.parking.repository;

import com.example.parking.entity.Vehicle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByUserIdOrderByPlateNumberAscIdAsc(Long userId);

    Optional<Vehicle> findByIdAndUserId(Long id, Long userId);
}
