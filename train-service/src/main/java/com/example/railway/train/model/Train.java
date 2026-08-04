package com.example.railway.train.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "trains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Train number is required")
    @Column(unique = true, nullable = false)
    private String trainNumber;

    @NotBlank(message = "Train name is required")
    @Column(nullable = false)
    private String trainName;

    @NotNull(message = "Train type is required")
    @Enumerated(EnumType.STRING)
    private TrainType trainType;

    @Min(value = 1, message = "Total seats must be at least 1")
    private int totalSeats;

    private int availableSeats;

    public enum TrainType {
        EXPRESS, SUPERFAST, LOCAL, RAJDHANI, SHATABDI
    }
}
