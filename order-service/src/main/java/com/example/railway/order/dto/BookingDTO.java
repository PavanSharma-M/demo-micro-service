package com.example.railway.order.dto;

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
public class BookingDTO {

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
    private BookingStatus status;
    private BigDecimal totalAmount;

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, FAILED
    }
}