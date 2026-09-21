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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_session_id", nullable = false, updatable = false, unique = true)
    private ParkingSession session;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;

    protected Payment() {
    }

    /** Records a successful payment of the session's final amount. */
    public Payment(ParkingSession session, Instant paidAt) {
        this.session = session;
        this.amount = session.getAmount();
        this.status = PaymentStatus.PAID;
        this.paidAt = paidAt;
    }

    public Long getId() {
        return id;
    }

    public ParkingSession getSession() {
        return session;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
