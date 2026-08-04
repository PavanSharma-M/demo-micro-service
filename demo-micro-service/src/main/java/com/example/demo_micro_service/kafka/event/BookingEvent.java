package com.example.demo_micro_service.kafka.event;

import com.example.demo_micro_service.model.Booking.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka event payload published whenever a booking is CREATED or CANCELLED.
 *
 * This is a plain Java record serialized to JSON and sent over the wire.
 * Both Producer and Consumer use the same class, so the schema is always in sync.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEvent {

    /** Unique event identifier (UUID). */
    private String eventId;

    /**
     * Saga correlation ID — equals the bookingNumber.
     * Every event in the saga chain carries the same sagaId so handlers
     * can look up the correct Booking record in the DB.
     */
    private String sagaId;

    /** Type of event: "BOOKING_CREATED" or "BOOKING_CANCELLED". */
    private String eventType;

    /** When this event was produced. */
    private LocalDateTime occurredAt;

    // ─── Booking snapshot ─────────────────────────────────────────

    private Long bookingId;
    private String bookingNumber;
    private Long routeId;
    /** Needed by SeatReservationSagaHandler (Step 2) to find and update the Train. */
    private Long trainId;
    private String trainName;
    private String trainNumber;
    private String sourceStation;
    private String destinationStation;
    private LocalDateTime departureTime;

    // ─── Passenger info ───────────────────────────────────────────

    private String passengerName;
    private String passengerEmail;
    private String passengerPhone;

    // ─── Booking details ──────────────────────────────────────────

    private int seatCount;
    private BigDecimal totalAmount;
    private BookingStatus status;
}
