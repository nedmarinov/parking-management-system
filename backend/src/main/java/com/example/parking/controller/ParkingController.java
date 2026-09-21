package com.example.parking.controller;

import com.example.parking.dto.ParkingResponse;
import com.example.parking.dto.StartParkingRequest;
import com.example.parking.dto.UserActionRequest;
import com.example.parking.service.ParkingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParkingController {

    private final ParkingService parkingService;

    public ParkingController(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    @PostMapping("/api/parkings")
    @ResponseStatus(HttpStatus.CREATED)
    public ParkingResponse start(@Valid @RequestBody StartParkingRequest request) {
        return parkingService.start(request.userId(), request.vehicleId(), request.zoneId());
    }

    @GetMapping("/api/users/{userId}/parkings/active")
    public List<ParkingResponse> listActive(@PathVariable @Positive Long userId) {
        return parkingService.listActive(userId);
    }

    @PostMapping("/api/parkings/{parkingId}/stop")
    public ParkingResponse stop(@PathVariable @Positive Long parkingId, @Valid @RequestBody UserActionRequest request) {
        return parkingService.stop(parkingId, request.userId());
    }
}
