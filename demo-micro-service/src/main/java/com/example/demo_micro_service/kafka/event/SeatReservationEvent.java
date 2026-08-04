package com.example.demo_micro_service.kafka.event;

import lombok.*;

/**
 * SAGA STEP EVENT — Seat Reservation
 * ────────────────────────────────────
 * Published by SeatReservationSagaHandler as the REPLY to a "booking-initiated" event.
 *
 * Two possible outcomes:
 *   eventType = "SEAT_RESERVED"           → saga proceeds to confirmation
 *   eventType = "SEAT_RESERVATION_FAILED" → saga triggers compensation (booking = FAILED)
 *
 * The sagaId is the bookingNumber — it acts as the CORRELATION ID that ties
 * all events in this saga chain together, just like a transaction ID.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatReservationEvent {

    /** Unique ID of this event (UUID). */
    private String eventId;

    /**
     * Correlation ID = bookingNumber.
     * BookingSagaCompletionHandler uses this to find the right Booking in the DB.
     */
    private String sagaId;

    /**
     * "SEAT_RESERVED"           — seats were successfully deducted from the train.
     * "SEAT_RESERVATION_FAILED" — not enough seats; compensation required.
     */
    private String eventType;

    /** The train whose seats were (or could not be) reserved. */
    private Long trainId;

    /** How many seats were attempted. */
    private int seatCount;

    /** Human-readable failure reason (only set when eventType = SEAT_RESERVATION_FAILED). */
    private String failureReason;
}
