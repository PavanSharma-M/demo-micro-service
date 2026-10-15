package com.example.railway.order.dto;

import com.example.railway.order.model.Order;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateDTO {

    @NotNull(message = "Order status is required")
    private Order.OrderStatus status;

    private String currentLocation;
}