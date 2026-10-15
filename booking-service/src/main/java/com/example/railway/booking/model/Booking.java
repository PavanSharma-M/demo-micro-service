package com.example.railway.booking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Booking entity — stores only routeId as a cross-service reference.
 * Full route/train details are fetched on-demand from route-service via Feign.
 */
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

    /** Saga correlation ID = bookingNumber, used to match Kafka reply events. */
    @Column(unique = true)
    private String sagaId;

    /** ID of the route in route-service */
    @Column(nullable = false)
    private Long routeId;

    /** ID of the train used for seat reservation and compensation. */
    @Column(nullable = false)
    private Long trainId;

    @NotBlank(message = "Passenger name is required")
    private String passengerName;

    @NotBlank(message = "Passenger email is required")
    @Email(message = "Invalid email format")
    private String passengerEmail;

    @NotBlank(message = "Passenger phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String passengerPhone;

    @Min(value = 1)
    @Max(value = 6)
    private int seatCount;

    private LocalDateTime bookingDate;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(length = 100)
    private String walletProvider;

    @Column(length = 100)
    private String paymentReference;

    @Column(length = 50)
    private String paymentTransactionId;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** Populated when saga compensation fires (booking fails). */
    @Column(length = 500)
    private String failureReason;

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, FAILED
    }

    public enum PaymentMethod {
        UPI, CREDIT_CARD, WALLET
    }

    public enum PaymentStatus {
        PENDING, PROCESSING, PAID, FAILED
    }
}
