package com.example.railway.route.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Route entity — stores only IDs referencing train-service and station-service.
 * No JPA relationships across microservice boundaries.
 */
@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the train in train-service */
    @NotNull(message = "Train ID is required")
    @Column(nullable = false)
    private Long trainId;

    /** ID of the source station in station-service */
    @NotNull(message = "Source station ID is required")
    @Column(nullable = false)
    private Long sourceStationId;

    /** ID of the destination station in station-service */
    @NotNull(message = "Destination station ID is required")
    @Column(nullable = false)
    private Long destinationStationId;

    @NotNull(message = "Departure time is required")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalDateTime arrivalTime;

    @Min(value = 1, message = "Distance must be at least 1 km")
    private int distanceKm;

    @DecimalMin(value = "0.0", inclusive = false, message = "Fare must be positive")
    @Column(precision = 10, scale = 2)
    private BigDecimal fare;
}
