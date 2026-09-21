package com.example.parking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.example.parking.entity.City;
import com.example.parking.entity.ParkingSession;
import com.example.parking.entity.ParkingZone;
import com.example.parking.entity.Payment;
import com.example.parking.entity.SessionStatus;
import com.example.parking.entity.User;
import com.example.parking.entity.Vehicle;
import com.example.parking.repository.CityRepository;
import com.example.parking.repository.ParkingSessionRepository;
import com.example.parking.repository.ParkingZoneRepository;
import com.example.parking.repository.PaymentRepository;
import com.example.parking.repository.UserRepository;
import com.example.parking.repository.VehicleRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabaseConfig.class)
@Transactional
class RepositoryIT {

    private static final Instant START = Instant.parse("2026-01-01T10:00:00.123Z");

    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository users;
    @Autowired private VehicleRepository vehicles;
    @Autowired private CityRepository cities;
    @Autowired private ParkingZoneRepository zones;
    @Autowired private ParkingSessionRepository sessions;
    @Autowired private PaymentRepository payments;

    @Test
    void catalogQueriesUseDocumentedOrderAndFilters() {
        assertThat(users.findAllByOrderByNameAscIdAsc()).extracting(User::getName)
                .containsExactly("Alex Johnson", "Maria Smith");
        assertThat(cities.findAllByOrderByNameAscIdAsc()).extracting(City::getName)
                .containsExactly("Plovdiv", "Sofia");
        assertThat(zones.findByCityIdAndActiveTrueOrderByNameAscIdAsc(2L)).extracting(ParkingZone::getName)
                .containsExactly("Blue Zone", "Green Zone");
        assertThat(vehicles.findByUserIdOrderByPlateNumberAscIdAsc(1L)).extracting(Vehicle::getPlateNumber)
                .containsExactly("CA1234AB", "CB5678CD");
    }

    @Test
    void vehicleLookupIsScopedToOwner() {
        assertThat(vehicles.findByIdAndUserId(3L, 2L)).isPresent();
        assertThat(vehicles.findByIdAndUserId(1L, 2L)).isEmpty();
    }

    @Test
    void sessionLifecycleAndPaymentRoundTrip() {
        ParkingSession session = sessions.saveAndFlush(start(1L, 1L, START));
        em.clear();

        List<ParkingSession> active = sessions.findWithDetailsByUserIdAndStatus(1L, SessionStatus.ACTIVE);
        assertThat(active).hasSize(1);
        ParkingSession loaded = active.get(0);
        assertThat(loaded.getHourlyRate()).isEqualByComparingTo("2.00");
        assertThat(loaded.getStartedAt()).isEqualTo(START);
        assertThat(Hibernate.isInitialized(loaded.getVehicle())).isTrue();
        assertThat(Hibernate.isInitialized(loaded.getZone())).isTrue();
        assertThat(Hibernate.isInitialized(loaded.getZone().getCity())).isTrue();

        ParkingSession locked = sessions.findByIdForUpdate(session.getId()).orElseThrow();
        locked.complete(START.plusSeconds(3600), new BigDecimal("2.00"));
        payments.saveAndFlush(new Payment(locked, START.plusSeconds(3700)));
        em.clear();

        assertThat(sessions.findWithDetailsByUserIdAndStatus(1L, SessionStatus.ACTIVE)).isEmpty();
        assertThat(sessions.findWithDetailsByUserIdAndStatus(1L, SessionStatus.COMPLETED))
                .extracting(ParkingSession::getId, ParkingSession::getAmount)
                .containsExactly(tuple(session.getId(), new BigDecimal("2.00")));
        assertThat(payments.existsBySessionId(session.getId())).isTrue();
        assertThat(payments.findBySessionIdIn(List.of(session.getId()))).extracting(Payment::getAmount)
                .containsExactly(new BigDecimal("2.00"));
    }

    @Test
    void sessionsListNewestFirstAndOnlyForUser() {
        ParkingSession older = sessions.save(start(1L, 1L, START));
        ParkingSession newer = sessions.save(start(1L, 2L, START.plusSeconds(60)));
        sessions.save(start(2L, 3L, START.plusSeconds(120)));
        sessions.flush();

        assertThat(sessions.findWithDetailsByUserIdAndStatus(1L, SessionStatus.ACTIVE))
                .extracting(ParkingSession::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void userLockBlocksOtherTransactions() {
        assertThat(users.findByIdForUpdate(1L)).isPresent();

        CompletableFuture<Object> competitor = CompletableFuture.supplyAsync(
                () -> jdbc.queryForObject("SELECT id FROM users WHERE id = 1 FOR UPDATE NOWAIT", Long.class));

        assertThatThrownBy(competitor::join).rootCause().hasMessageContaining("could not obtain lock");
    }

    private ParkingSession start(long userId, long vehicleId, Instant startedAt) {
        Vehicle vehicle = vehicles.findByIdAndUserId(vehicleId, userId).orElseThrow();
        return new ParkingSession(vehicle.getUser(), vehicle, zones.getReferenceById(1L), startedAt);
    }
}
