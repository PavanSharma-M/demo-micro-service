package com.example.railway.order.client;

import com.example.railway.order.config.FeignConfig;
import com.example.railway.order.dto.BookingDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booking-service", configuration = FeignConfig.class)
public interface BookingClient {

    @GetMapping("/api/v1/bookings/{id}")
    BookingDTO getBookingById(@PathVariable Long id);
}