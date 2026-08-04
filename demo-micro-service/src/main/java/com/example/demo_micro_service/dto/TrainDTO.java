package com.example.demo_micro_service.dto;

import com.example.demo_micro_service.model.Train;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainDTO {

    private Long id;

    @NotBlank(message = "Train number is required")
    private String trainNumber;

    @NotBlank(message = "Train name is required")
    private String trainName;

    @NotNull(message = "Train type is required")
    private Train.TrainType trainType;

    @Min(value = 1, message = "Total seats must be at least 1")
    private int totalSeats;

    private int availableSeats;
}
