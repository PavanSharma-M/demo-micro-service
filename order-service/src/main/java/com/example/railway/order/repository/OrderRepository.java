package com.example.railway.order.repository;

import com.example.railway.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByTransactionId(String transactionId);

    List<Order> findByBookingId(Long bookingId);

    List<Order> findByCustomerEmail(String customerEmail);
}