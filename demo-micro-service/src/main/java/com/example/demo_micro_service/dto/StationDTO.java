package com.example.demo_micro_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationDTO {

    private Long id;

    @NotBlank(message = "Station code is required")
    private String stationCode;

    @NotBlank(message = "Station name is required")
    private String stationName;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;
}
