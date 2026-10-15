package com.example.railway.order.controller;

import com.example.railway.order.dto.OrderRequestDTO;
import com.example.railway.order.dto.OrderResponseDTO;
import com.example.railway.order.dto.OrderStatusUpdateDTO;
import com.example.railway.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Tracking", description = "APIs for creating and tracking train-delivered orders")
@SecurityRequirement(name = "Keycloak")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a tracked order from a confirmed booking")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping
    @Operation(summary = "Get all tracked orders")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by internal ID")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Track order by transaction ID")
    public ResponseEntity<OrderResponseDTO> getOrderByTransactionId(@PathVariable String transactionId) {
        return ResponseEntity.ok(orderService.getOrderByTransactionId(transactionId));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get all orders created for a booking")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(orderService.getOrdersByBookingId(bookingId));
    }

    @GetMapping("/customer")
    @Operation(summary = "Get all orders for a customer email")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByCustomerEmail(@RequestParam String email) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerEmail(email));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update tracking status and current location")
    public ResponseEntity<OrderResponseDTO> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody OrderStatusUpdateDTO request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }
}