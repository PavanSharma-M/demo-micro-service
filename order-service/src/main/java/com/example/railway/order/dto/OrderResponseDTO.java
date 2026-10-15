package com.example.railway.order.dto;

import com.example.railway.order.model.Order;
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
public class OrderResponseDTO {

    private Long id;
    private String transactionId;
    private Long bookingId;
    private String bookingNumber;
    private String itemName;
    private String itemDescription;
    private int quantity;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String sourceStation;
    private String destinationStation;
    private String currentLocation;
    private String trainName;
    private String trainNumber;
    private LocalDateTime departureTime;
    private LocalDateTime expectedArrivalTime;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private String specialInstructions;
    private Order.OrderStatus status;
}