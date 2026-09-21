package com.example.parking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "parking_sessions")
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false, updatable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false, updatable = false)
    private ParkingZone zone;

    @Column(name = "hourly_rate", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SessionStatus status;

    protected ParkingSession() {
    }

    /** Starts an active session, capturing the zone's current hourly rate. */
    public ParkingSession(User user, Vehicle vehicle, ParkingZone zone, Instant startedAt) {
        this.user = user;
        this.vehicle = vehicle;
        this.zone = zone;
        this.hourlyRate = zone.getPricePerHour();
        this.startedAt = startedAt;
        this.status = SessionStatus.ACTIVE;
    }

    public void complete(Instant endedAt, BigDecimal amount) {
        this.endedAt = endedAt;
        this.amount = amount;
        this.status = SessionStatus.COMPLETED;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingZone getZone() {
        return zone;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SessionStatus getStatus() {
        return status;
    }
}
