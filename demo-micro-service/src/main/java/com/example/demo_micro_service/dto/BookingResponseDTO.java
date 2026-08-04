package com.example.demo_micro_service.dto;

import com.example.demo_micro_service.model.Booking;
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
    private BigDecimal totalAmount;
    /** Populated when status = FAILED — explains why the saga compensation was triggered. */
    private String failureReason;
}
