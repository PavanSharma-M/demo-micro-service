package com.example.railway.booking.dto;

import com.example.railway.booking.model.Train;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Feign response DTO — matches route-service RouteDTO */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteDTO {
    private Long id;
    private Long trainId;
    private String trainNumber;
    private String trainName;
    private Train.TrainType trainType;
    private int availableSeats;
    private Long sourceStationId;
    private String sourceStationCode;
    private String sourceStationName;
    private String sourceCity;
    private Long destinationStationId;
    private String destinationStationCode;
    private String destinationStationName;
    private String destinationCity;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private int distanceKm;
    private BigDecimal fare;
}
