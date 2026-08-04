package com.example.railway.booking.client;

import com.example.railway.booking.config.FeignConfig;
import com.example.railway.booking.dto.RouteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "route-service", configuration = FeignConfig.class)
public interface RouteClient {

    @GetMapping("/api/v1/routes/{id}")
    RouteDTO getRouteById(@PathVariable Long id);
}
