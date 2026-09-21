package com.example.parking.service;

import com.example.parking.dto.Money;
import com.example.parking.dto.PaymentResponse;
import com.example.parking.dto.Timestamps;
import com.example.parking.entity.ParkingSession;
import com.example.parking.entity.Payment;
import com.example.parking.entity.SessionStatus;
import com.example.parking.entity.User;
import com.example.parking.exception.ApiException;
import com.example.parking.exception.ErrorCode;
import com.example.parking.repository.PaymentRepository;
import com.example.parking.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final UserRepository users;
    private final PaymentRepository payments;
    private final ParkingService parkingService;
    private final Clock clock;

    public PaymentService(UserRepository users, PaymentRepository payments, ParkingService parkingService,
            Clock clock) {
        this.users = users;
        this.payments = payments;
        this.parkingService = parkingService;
        this.clock = clock;
    }

    /** Deducts the stored session amount and records the payment atomically, under user then session locks. */
    @Transactional
    public PaymentResponse pay(Long parkingId, Long userId) {
        User user = users.findByIdForUpdate(userId).orElseThrow(UserService::userNotFound);
        ParkingSession session = parkingService.lockOwnedSession(parkingId, userId);
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new ApiException(ErrorCode.PARKING_NOT_COMPLETED, "Parking must be stopped before payment");
        }
        BigDecimal amount = session.getAmount();
        if (amount == null) {
            throw new IllegalStateException("Completed parking " + parkingId + " has no amount");
        }
        if (payments.existsBySessionId(parkingId)) {
            throw new ApiException(ErrorCode.PARKING_ALREADY_PAID, "Parking is already paid");
        }
        Instant paidAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        if (paidAt.isBefore(session.getEndedAt())) {
            throw new ApiException(ErrorCode.CLOCK_BEFORE_SESSION_TIME, "Payment time is before the parking end");
        }
        if (user.getBalance().compareTo(amount) < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE, "Insufficient account balance");
        }
        user.setBalance(user.getBalance().subtract(amount));
        Payment payment = payments.save(new Payment(session, paidAt));
        return new PaymentResponse(parkingId, Money.format(payment.getAmount()), payment.getStatus().name(),
                Timestamps.format(payment.getPaidAt()), Money.format(user.getBalance()));
    }
}
