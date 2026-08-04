package com.example.railway.route.dto;

import lombok.*;

/** Feign response DTO — mirrors station-service StationDTO */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationDTO {
    private Long id;
    private String stationCode;
    private String stationName;
    private String city;
    private String state;
}
