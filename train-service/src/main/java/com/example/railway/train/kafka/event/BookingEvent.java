package com.example.railway.train.kafka.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka event received from booking-service when a new booking is initiated.
 * train-service consumes this to reserve seats (saga step 2).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEvent {

    private String eventId;

    /** Saga correlation ID = bookingNumber. Used in reply events. */
    private String sagaId;

    private String eventType;
    private LocalDateTime occurredAt;

    private Long bookingId;
    private String bookingNumber;
    private Long routeId;

    /** trainId — the train whose seats must be reserved. */
    private Long trainId;
    private String trainName;
    private String trainNumber;
    private String sourceStation;
    private String destinationStation;
    private LocalDateTime departureTime;

    private String passengerName;
    private String passengerEmail;
    private String passengerPhone;

    private int seatCount;
    private BigDecimal totalAmount;
    private String failureReason;
}
