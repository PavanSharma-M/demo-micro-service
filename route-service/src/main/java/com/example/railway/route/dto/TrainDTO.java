package com.example.railway.route.dto;

import com.example.railway.route.model.Train;
import lombok.*;

/** Feign response DTO — mirrors train-service TrainDTO */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainDTO {
    private Long id;
    private String trainNumber;
    private String trainName;
    private Train.TrainType trainType;
    private int totalSeats;
    private int availableSeats;
}
