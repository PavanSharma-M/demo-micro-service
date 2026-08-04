package com.example.demo_micro_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteDTO {

    private Long id;

    @NotNull(message = "Train ID is required")
    private Long trainId;

    private String trainNumber;
    private String trainName;

    @NotNull(message = "Source station ID is required")
    private Long sourceStationId;

    private String sourceStationCode;
    private String sourceStationName;

    @NotNull(message = "Destination station ID is required")
    private Long destinationStationId;

    private String destinationStationCode;
    private String destinationStationName;

    @NotNull(message = "Departure time is required")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalDateTime arrivalTime;

    @Min(value = 1, message = "Distance must be at least 1 km")
    private int distanceKm;

    @DecimalMin(value = "0.0", inclusive = false, message = "Fare must be positive")
    private BigDecimal fare;
}
