package com.example.railway.order.service;

import com.example.railway.order.client.BookingClient;
import com.example.railway.order.dto.BookingDTO;
import com.example.railway.order.dto.OrderRequestDTO;
import com.example.railway.order.dto.OrderResponseDTO;
import com.example.railway.order.dto.OrderStatusUpdateDTO;
import com.example.railway.order.exception.OrderException;
import com.example.railway.order.exception.ResourceNotFoundException;
import com.example.railway.order.model.Order;
import com.example.railway.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookingClient bookingClient;

    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        log.info("Creating order for bookingId={} item={} quantity={}",
                request.getBookingId(), request.getItemName(), request.getQuantity());

        BookingDTO booking = bookingClient.getBookingById(request.getBookingId());
        if (booking.getStatus() != BookingDTO.BookingStatus.CONFIRMED) {
            throw new OrderException("Order can only be created for a CONFIRMED booking");
        }

        Order order = Order.builder()
                .transactionId(generateTransactionId())
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .itemName(request.getItemName())
                .itemDescription(request.getItemDescription())
                .quantity(request.getQuantity())
                .customerName(booking.getPassengerName())
                .customerEmail(booking.getPassengerEmail())
                .customerPhone(booking.getPassengerPhone())
                .sourceStation(booking.getSourceStation())
                .destinationStation(booking.getDestinationStation())
                .currentLocation(booking.getSourceStation())
                .trainName(booking.getTrainName())
                .trainNumber(booking.getTrainNumber())
                .departureTime(booking.getDepartureTime())
                .expectedArrivalTime(booking.getArrivalTime())
                .createdAt(LocalDateTime.now())
                .lastUpdatedAt(LocalDateTime.now())
                .specialInstructions(request.getSpecialInstructions())
                .status(Order.OrderStatus.BOOKED)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order created transactionId={} bookingNumber={}",
                saved.getTransactionId(), saved.getBookingNumber());
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        return toResponseDTO(findById(id));
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByTransactionId(String transactionId) {
        return toResponseDTO(orderRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with transaction ID: " + transactionId)));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByBookingId(Long bookingId) {
        return orderRepository.findByBookingId(bookingId).stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByCustomerEmail(String email) {
        return orderRepository.findByCustomerEmail(email).stream().map(this::toResponseDTO).toList();
    }

    public OrderResponseDTO updateStatus(Long id, OrderStatusUpdateDTO request) {
        Order order = findById(id);
        validateStatusTransition(order, request.getStatus());

        order.setStatus(request.getStatus());
        order.setCurrentLocation(resolveCurrentLocation(order, request));
        order.setLastUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        log.info("Updated order transactionId={} status={} currentLocation={}",
                saved.getTransactionId(), saved.getStatus(), saved.getCurrentLocation());
        return toResponseDTO(saved);
    }

    private void validateStatusTransition(Order order, Order.OrderStatus nextStatus) {
        if (order.getStatus() == Order.OrderStatus.DELIVERED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new OrderException("Finalized order cannot be updated further");
        }
        if (order.getStatus() == nextStatus) {
            throw new OrderException("Order is already in status: " + nextStatus);
        }
    }

    private String resolveCurrentLocation(Order order, OrderStatusUpdateDTO request) {
        if (StringUtils.hasText(request.getCurrentLocation())) {
            return request.getCurrentLocation();
        }
        return switch (request.getStatus()) {
            case BOOKED -> order.getSourceStation();
            case IN_TRANSIT -> "On train " + order.getTrainNumber();
            case ARRIVED_AT_DESTINATION, DELIVERED -> order.getDestinationStation();
            case CANCELLED -> order.getCurrentLocation();
        };
    }

    private Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private String generateTransactionId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "TXN" + timestamp + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private OrderResponseDTO toResponseDTO(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .transactionId(order.getTransactionId())
                .bookingId(order.getBookingId())
                .bookingNumber(order.getBookingNumber())
                .itemName(order.getItemName())
                .itemDescription(order.getItemDescription())
                .quantity(order.getQuantity())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .sourceStation(order.getSourceStation())
                .destinationStation(order.getDestinationStation())
                .currentLocation(order.getCurrentLocation())
                .trainName(order.getTrainName())
                .trainNumber(order.getTrainNumber())
                .departureTime(order.getDepartureTime())
                .expectedArrivalTime(order.getExpectedArrivalTime())
                .createdAt(order.getCreatedAt())
                .lastUpdatedAt(order.getLastUpdatedAt())
                .specialInstructions(order.getSpecialInstructions())
                .status(order.getStatus())
                .build();
    }
}