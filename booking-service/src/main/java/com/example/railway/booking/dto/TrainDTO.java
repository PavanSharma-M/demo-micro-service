package com.example.railway.booking.dto;

import com.example.railway.booking.model.Train;
import lombok.*;

/** Feign response DTO — matches train-service TrainDTO */
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
