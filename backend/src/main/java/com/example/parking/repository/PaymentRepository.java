package com.example.parking.repository;

import com.example.parking.entity.Payment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsBySessionId(Long sessionId);

    List<Payment> findBySessionIdIn(Collection<Long> sessionIds);
}
