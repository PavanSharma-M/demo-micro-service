package com.example.railway.booking.kafka.event;

import lombok.*;

/**
 * Saga reply event published by train-service after attempting seat reservation.
 *
 * eventType = "SEAT_RESERVED"           → saga step 3a: confirm booking
 * eventType = "SEAT_RESERVATION_FAILED" → saga step 3b: compensation, mark booking FAILED
 *
 * The sagaId (= bookingNumber) is the correlation ID that lets booking-service
 * match this reply to the correct pending Booking record.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatReservationEvent {

    private String eventId;

    /**
     * Correlation ID = bookingNumber.
     * booking-service uses this to find the right Booking record.
     */
    private String sagaId;

    /** "SEAT_RESERVED" or "SEAT_RESERVATION_FAILED" */
    private String eventType;

    private Long trainId;
    private int seatCount;

    /** Failure reason — only set when eventType = SEAT_RESERVATION_FAILED. */
    private String failureReason;
}
