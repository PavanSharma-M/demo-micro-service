package com.example.railway.booking.kafka.event;

import com.example.railway.booking.model.Booking.BookingStatus;
import com.example.railway.booking.model.Booking.PaymentMethod;
import com.example.railway.booking.model.Booking.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka event payload published by booking-service and consumed by train-service.
 *
 * Used for:
 *  - booking-initiated  (saga step 1 → 2)
 *  - booking-created    (notification after confirmation)
 *  - booking-cancelled  (notification)
 *  - booking-confirmed  (saga final success)
 *  - booking-failed     (saga final failure)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEvent {

    /** Unique event ID (UUID). */
    private String eventId;

    /**
     * Saga correlation ID = bookingNumber.
     * Every event in the saga chain carries the same sagaId so handlers
     * can look up the correct Booking record in the DB.
     */
    private String sagaId;

    /** Event type: BOOKING_INITIATED, BOOKING_CREATED, BOOKING_CANCELLED, etc. */
    private String eventType;

    private LocalDateTime occurredAt;

    // ── Booking snapshot ─────────────────────────────────────────────

    private Long bookingId;
    private String bookingNumber;
    private Long routeId;

    /** TrainId — needed by train-service (Step 2) to deduct seats. */
    private Long trainId;
    private String trainName;
    private String trainNumber;
    private String sourceStation;
    private String destinationStation;
    private LocalDateTime departureTime;

    // ── Passenger info ────────────────────────────────────────────────

    private String passengerName;
    private String passengerEmail;
    private String passengerPhone;

    // ── Booking details ───────────────────────────────────────────────

    private int seatCount;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String paymentReference;
    private String walletProvider;
    private String paymentTransactionId;
    private String failureReason;
}
