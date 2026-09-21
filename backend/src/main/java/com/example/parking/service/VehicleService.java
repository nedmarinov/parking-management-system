package com.example.parking.service;

import com.example.parking.dto.VehicleResponse;
import com.example.parking.repository.UserRepository;
import com.example.parking.repository.VehicleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {

    private final UserRepository users;
    private final VehicleRepository vehicles;

    public VehicleService(UserRepository users, VehicleRepository vehicles) {
        this.users = users;
        this.vehicles = vehicles;
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> listUserVehicles(Long userId) {
        if (!users.existsById(userId)) {
            throw UserService.userNotFound();
        }
        return vehicles.findByUserIdOrderByPlateNumberAscIdAsc(userId).stream().map(VehicleResponse::from).toList();
    }
}
