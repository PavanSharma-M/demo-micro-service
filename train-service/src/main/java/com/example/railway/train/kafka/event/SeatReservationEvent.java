package com.example.railway.train.kafka.event;

import lombok.*;

/**
 * Saga reply event published by train-service after attempting seat reservation.
 * booking-service consumes this to finalize the saga (step 3).
 *
 * eventType = "SEAT_RESERVED"           → booking confirmed
 * eventType = "SEAT_RESERVATION_FAILED" → compensation triggered
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatReservationEvent {

    private String eventId;

    /** Correlation ID = bookingNumber. booking-service uses this to find the Booking. */
    private String sagaId;

    /** "SEAT_RESERVED" or "SEAT_RESERVATION_FAILED" */
    private String eventType;

    private Long trainId;
    private int seatCount;

    /** Only populated when eventType = SEAT_RESERVATION_FAILED. */
    private String failureReason;
}
