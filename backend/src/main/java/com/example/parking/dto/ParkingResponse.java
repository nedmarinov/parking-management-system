package com.example.parking.dto;

import com.example.parking.entity.ParkingSession;
import com.example.parking.entity.ParkingZone;
import com.example.parking.entity.Payment;
import com.example.parking.entity.SessionStatus;

public record ParkingResponse(
        Long id,
        Long userId,
        VehicleResponse vehicle,
        ZoneSummary zone,
        String hourlyRate,
        String startedAt,
        String endedAt,
        String amount,
        String status,
        String paymentStatus,
        String paidAt) {

    /** Zone identity only; the session's captured rate is the one that applies. */
    public record ZoneSummary(Long id, String name, CityResponse city) {
    }

    /** Maps a session and its payment, if any; requires vehicle, zone, and city to be loadable. */
    public static ParkingResponse from(ParkingSession session, Payment payment) {
        ParkingZone zone = session.getZone();
        String paymentStatus = session.getStatus() == SessionStatus.ACTIVE ? null
                : payment == null ? "UNPAID" : payment.getStatus().name();
        return new ParkingResponse(
                session.getId(),
                session.getUser().getId(),
                VehicleResponse.from(session.getVehicle()),
                new ZoneSummary(zone.getId(), zone.getName(), CityResponse.from(zone.getCity())),
                Money.format(session.getHourlyRate()),
                Timestamps.format(session.getStartedAt()),
                Timestamps.format(session.getEndedAt()),
                Money.format(session.getAmount()),
                session.getStatus().name(),
                paymentStatus,
                payment == null ? null : Timestamps.format(payment.getPaidAt()));
    }
}
