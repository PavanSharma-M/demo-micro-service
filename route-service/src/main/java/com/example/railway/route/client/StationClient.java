package com.example.railway.route.client;

import com.example.railway.route.config.FeignConfig;
import com.example.railway.route.dto.StationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "station-service", configuration = FeignConfig.class)
public interface StationClient {

    @GetMapping("/api/v1/stations/{id}")
    StationDTO getStationById(@PathVariable Long id);

    @GetMapping("/api/v1/stations/code/{stationCode}")
    StationDTO getStationByCode(@PathVariable String stationCode);
}
