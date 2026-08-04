package com.example.demo_micro_service.repository;

import com.example.demo_micro_service.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingNumber(String bookingNumber);

    /** Used by saga handlers to correlate Kafka reply events back to a Booking. */
    Optional<Booking> findBySagaId(String sagaId);

    List<Booking> findByPassengerEmail(String passengerEmail);

    List<Booking> findByRouteId(Long routeId);

    List<Booking> findByStatus(Booking.BookingStatus status);
}
