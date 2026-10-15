package com.example.railway.booking.kafka.event;

import com.example.railway.booking.model.Booking.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResultEvent {

    private String eventId;
    private String sagaId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long bookingId;
    private String bookingNumber;
    private PaymentMethod paymentMethod;
    private String paymentTransactionId;
    private String failureReason;
}