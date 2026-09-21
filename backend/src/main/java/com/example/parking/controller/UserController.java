package com.example.parking.controller;

import com.example.parking.dto.BalanceResponse;
import com.example.parking.dto.TopUpRequest;
import com.example.parking.dto.UserResponse;
import com.example.parking.dto.VehicleResponse;
import com.example.parking.service.TopUpAmount;
import com.example.parking.service.UserService;
import com.example.parking.service.VehicleService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final VehicleService vehicleService;

    public UserController(UserService userService, VehicleService vehicleService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userService.listUsers();
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable @Positive Long userId) {
        return userService.getUser(userId);
    }

    @PostMapping("/{userId}/top-up")
    public BalanceResponse topUp(@PathVariable @Positive Long userId, @RequestBody TopUpRequest request) {
        return userService.topUp(userId, TopUpAmount.parse(request.amount()));
    }

    @GetMapping("/{userId}/vehicles")
    public List<VehicleResponse> listVehicles(@PathVariable @Positive Long userId) {
        return vehicleService.listUserVehicles(userId);
    }
}
