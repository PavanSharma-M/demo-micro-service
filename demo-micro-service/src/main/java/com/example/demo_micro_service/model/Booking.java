package com.example.demo_micro_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String bookingNumber;

    @NotNull(message = "Route is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @NotBlank(message = "Passenger name is required")
    private String passengerName;

    @NotBlank(message = "Passenger email is required")
    @Email(message = "Invalid email format")
    private String passengerEmail;

    @NotBlank(message = "Passenger phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String passengerPhone;

    @Min(value = 1, message = "Seat count must be at least 1")
    @Max(value = 6, message = "Cannot book more than 6 seats at once")
    private int seatCount;

    private LocalDateTime bookingDate;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Saga correlation ID — used to match Kafka reply events back to this booking.
     * Set to the bookingNumber so every saga event carries the same ID.
     */
    @Column(unique = true)
    private String sagaId;

    /** Reason populated when status = FAILED (e.g. "Not enough seats available"). */
    private String failureReason;

    public enum BookingStatus {
        /**  Booking created, saga in progress — seats not yet reserved. */
        PENDING,
        /** Saga completed successfully — seats reserved, booking confirmed. */
        CONFIRMED,
        /** Booking was cancelled after confirmation — seats released. */
        CANCELLED,
        /** Saga compensation triggered — booking could not be completed. */
        FAILED
    }
}
