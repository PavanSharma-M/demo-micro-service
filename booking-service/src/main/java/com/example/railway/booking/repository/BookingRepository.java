package com.example.railway.booking.repository;

import com.example.railway.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingNumber(String bookingNumber);
    Optional<Booking> findBySagaId(String sagaId);
    List<Booking> findByPassengerEmail(String email);
}
