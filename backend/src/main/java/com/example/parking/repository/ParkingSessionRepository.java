package com.example.parking.repository;

import com.example.parking.entity.ParkingSession;
import com.example.parking.entity.SessionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    /** Newest first, with vehicle, zone, and city fetched for response mapping. */
    @Query("""
            select s from ParkingSession s
            join fetch s.vehicle
            join fetch s.zone z
            join fetch z.city
            where s.user.id = :userId and s.status = :status
            order by s.startedAt desc, s.id desc""")
    List<ParkingSession> findWithDetailsByUserIdAndStatus(Long userId, SessionStatus status);

    boolean existsByVehicleIdAndStatus(Long vehicleId, SessionStatus status);

    /** Whether the vehicle has a completed session without a payment. */
    @Query("""
            select count(s) > 0 from ParkingSession s
            where s.vehicle.id = :vehicleId
              and s.status = com.example.parking.entity.SessionStatus.COMPLETED
              and not exists (select p.id from Payment p where p.session = s)""")
    boolean existsUnpaidByVehicleId(Long vehicleId);

    /** Take only after the owning user's lock (user, then session). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ParkingSession s where s.id = :id")
    Optional<ParkingSession> findByIdForUpdate(Long id);
}
