package com.example.parking.service;

import com.example.parking.dto.Money;
import com.example.parking.dto.ParkingResponse;
import com.example.parking.entity.ParkingSession;
import com.example.parking.entity.ParkingZone;
import com.example.parking.entity.SessionStatus;
import com.example.parking.entity.User;
import com.example.parking.entity.Vehicle;
import com.example.parking.exception.ApiException;
import com.example.parking.exception.ErrorCode;
import com.example.parking.repository.ParkingSessionRepository;
import com.example.parking.repository.ParkingZoneRepository;
import com.example.parking.repository.UserRepository;
import com.example.parking.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingService {

    private final UserRepository users;
    private final VehicleRepository vehicles;
    private final ParkingZoneRepository zones;
    private final ParkingSessionRepository sessions;
    private final PricingService pricing;
    private final Clock clock;

    public ParkingService(UserRepository users, VehicleRepository vehicles, ParkingZoneRepository zones,
            ParkingSessionRepository sessions, PricingService pricing, Clock clock) {
        this.users = users;
        this.vehicles = vehicles;
        this.zones = zones;
        this.sessions = sessions;
        this.pricing = pricing;
        this.clock = clock;
    }

    @Transactional
    public ParkingResponse start(Long userId, Long vehicleId, Long zoneId) {
        User user = users.findByIdForUpdate(userId).orElseThrow(UserService::userNotFound);
        Vehicle vehicle = vehicles.findById(vehicleId)
                .orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
        if (!vehicle.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.VEHICLE_NOT_OWNED, "Vehicle belongs to another user");
        }
        ParkingZone zone = zones.findById(zoneId)
                .orElseThrow(() -> new ApiException(ErrorCode.ZONE_NOT_FOUND, "Zone not found"));
        if (!zone.isActive()) {
            throw new ApiException(ErrorCode.ZONE_INACTIVE, "Zone is not active");
        }
        if (sessions.existsByVehicleIdAndStatus(vehicleId, SessionStatus.ACTIVE)) {
            throw vehicleAlreadyParked();
        }
        ParkingSession session = sessions.saveAndFlush(new ParkingSession(user, vehicle, zone, now()));
        return ParkingResponse.from(session, null);
    }

    @Transactional
    public ParkingResponse stop(Long parkingId, Long userId) {
        users.findByIdForUpdate(userId).orElseThrow(UserService::userNotFound);
        ParkingSession session = lockOwnedSession(parkingId, userId);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ApiException(ErrorCode.PARKING_ALREADY_COMPLETED, "Parking is already completed");
        }
        Instant endedAt = now();
        BigDecimal amount = pricing.calculate(session.getHourlyRate(), session.getStartedAt(), endedAt);
        if (amount.compareTo(Money.MAX) > 0) {
            throw new ApiException(ErrorCode.AMOUNT_LIMIT_EXCEEDED, "Parking amount exceeds the storage limit");
        }
        session.complete(endedAt, amount);
        return ParkingResponse.from(session, null);
    }

    @Transactional(readOnly = true)
    public List<ParkingResponse> listActive(Long userId) {
        if (!users.existsById(userId)) {
            throw UserService.userNotFound();
        }
        return sessions.findWithDetailsByUserIdAndStatus(userId, SessionStatus.ACTIVE).stream()
                .map(s -> ParkingResponse.from(s, null))
                .toList();
    }

    /** Locks the session after the user lock, reporting ownership before lifecycle state. */
    ParkingSession lockOwnedSession(Long parkingId, Long userId) {
        ParkingSession session = sessions.findByIdForUpdate(parkingId)
                .orElseThrow(() -> new ApiException(ErrorCode.PARKING_NOT_FOUND, "Parking not found"));
        if (!session.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PARKING_NOT_OWNED, "Parking belongs to another user");
        }
        return session;
    }

    static ApiException vehicleAlreadyParked() {
        return new ApiException(ErrorCode.VEHICLE_ALREADY_PARKED, "Vehicle already has an active parking");
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
