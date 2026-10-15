package com.example.railway.booking.dto;

import com.example.railway.booking.model.Booking;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {

    private Long id;
    private String bookingNumber;
    private Long routeId;
    private String trainName;
    private String trainNumber;
    private String sourceStation;
    private String destinationStation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String passengerName;
    private String passengerEmail;
    private String passengerPhone;
    private int seatCount;
    private LocalDateTime bookingDate;
    private Booking.BookingStatus status;
    private Booking.PaymentMethod paymentMethod;
    private Booking.PaymentStatus paymentStatus;
    private String paymentReference;
    private String paymentTransactionId;
    private BigDecimal totalAmount;
    private String failureReason;
}
