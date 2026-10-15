package com.example.railway.booking.kafka.event;

import com.example.railway.booking.model.Booking.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestEvent {

    private String eventId;
    private String sagaId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long bookingId;
    private String bookingNumber;
    private String passengerEmail;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String paymentReference;
    private String walletProvider;
}