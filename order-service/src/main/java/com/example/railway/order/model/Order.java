package com.example.railway.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String transactionId;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private String bookingNumber;

    @Column(nullable = false)
    private String itemName;

    @Column(length = 1000)
    private String itemDescription;

    private int quantity;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String customerPhone;

    @Column(nullable = false)
    private String sourceStation;

    @Column(nullable = false)
    private String destinationStation;

    @Column(nullable = false)
    private String currentLocation;

    @Column(nullable = false)
    private String trainName;

    @Column(nullable = false)
    private String trainNumber;

    private LocalDateTime departureTime;

    private LocalDateTime expectedArrivalTime;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;

    @Column(length = 1000)
    private String specialInstructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    public enum OrderStatus {
        BOOKED,
        IN_TRANSIT,
        ARRIVED_AT_DESTINATION,
        DELIVERED,
        CANCELLED
    }
}