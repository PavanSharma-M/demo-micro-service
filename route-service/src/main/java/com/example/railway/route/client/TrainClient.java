package com.example.railway.route.client;

import com.example.railway.route.config.FeignConfig;
import com.example.railway.route.dto.TrainDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "train-service", configuration = FeignConfig.class)
public interface TrainClient {

    @GetMapping("/api/v1/trains/{id}")
    TrainDTO getTrainById(@PathVariable Long id);
}
