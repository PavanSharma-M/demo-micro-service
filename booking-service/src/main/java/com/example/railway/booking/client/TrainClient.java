package com.example.railway.booking.client;

import com.example.railway.booking.config.FeignConfig;
import com.example.railway.booking.dto.TrainDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "train-service", configuration = FeignConfig.class)
public interface TrainClient {

    /**
     * Updates available seats by delta.
     * Negative delta = reserve seats (booking).
     * Positive delta = release seats (cancellation).
     */
    @PatchMapping("/api/v1/trains/{id}/seats")
    TrainDTO updateSeats(@PathVariable Long id, @RequestParam int delta);
}
