package com.example.parking.dto;

public record PaymentResponse(Long parkingId, String amount, String paymentStatus, String paidAt,
        String remainingBalance) {
}
