package com.example.railway.route.dto;

import com.example.railway.route.model.Train;
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

    // Populated from train-service via Feign
    private String trainNumber;
    private String trainName;
    private Train.TrainType trainType;
    private int availableSeats;

    @NotNull(message = "Source station ID is required")
    private Long sourceStationId;

    // Populated from station-service via Feign
    private String sourceStationCode;
    private String sourceStationName;
    private String sourceCity;

    @NotNull(message = "Destination station ID is required")
    private Long destinationStationId;

    private String destinationStationCode;
    private String destinationStationName;
    private String destinationCity;

    @NotNull(message = "Departure time is required")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalDateTime arrivalTime;

    @Min(value = 1, message = "Distance must be at least 1 km")
    private int distanceKm;

    @DecimalMin(value = "0.0", inclusive = false, message = "Fare must be positive")
    private BigDecimal fare;
}
