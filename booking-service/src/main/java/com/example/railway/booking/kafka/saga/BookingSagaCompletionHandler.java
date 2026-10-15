package com.example.railway.booking.kafka.saga;

import com.example.railway.booking.kafka.event.BookingEvent;
import com.example.railway.booking.kafka.event.PaymentRequestEvent;
import com.example.railway.booking.kafka.event.PaymentResultEvent;
import com.example.railway.booking.kafka.producer.BookingEventProducer;
import com.example.railway.booking.kafka.event.SeatReservationEvent;
import com.example.railway.booking.model.Booking;
import com.example.railway.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║   SAGA STEP 3 — Booking Saga Completion Handler                 ║
 * ║   Lives in: booking-service                                     ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Listens to replies from train-service:
 *
 *   "seat-reserved"           → Step 3a: mark Booking CONFIRMED
 *   "seat-reservation-failed" → Step 3b: compensation — mark Booking FAILED
 *
 * The sagaId (= bookingNumber) is the correlation ID used to find the
 * pending Booking record created in Step 1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSagaCompletionHandler {

    private final BookingRepository bookingRepository;
    private final BookingEventProducer bookingEventProducer;

    /**
     * SAGA STEP 3a — Seat reserved.
     * Request payment processing as the next step in the saga.
     */
    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.seat-reserved}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "seatReservationKafkaListenerContainerFactory"
    )
    public void handleSeatReserved(@Payload SeatReservationEvent event) {
        log.info("SAGA STEP 3a ▶ Seats reserved, requesting payment | sagaId={}", event.getSagaId());

        bookingRepository.findBySagaId(event.getSagaId()).ifPresentOrElse(booking -> {
            booking.setPaymentStatus(Booking.PaymentStatus.PROCESSING);
            bookingRepository.save(booking);
            bookingEventProducer.publishPaymentRequested(PaymentRequestEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sagaId(booking.getSagaId())
                    .eventType("PAYMENT_REQUESTED")
                    .occurredAt(LocalDateTime.now())
                    .bookingId(booking.getId())
                    .bookingNumber(booking.getBookingNumber())
                    .passengerEmail(booking.getPassengerEmail())
                    .amount(booking.getTotalAmount())
                    .paymentMethod(booking.getPaymentMethod())
                    .paymentReference(booking.getPaymentReference())
                    .walletProvider(booking.getWalletProvider())
                    .build());
            log.info("SAGA STEP 3a ✔ Payment requested | sagaId={} | bookingNumber={}",
                    event.getSagaId(), booking.getBookingNumber());
        }, () -> log.warn("SAGA STEP 3a ✘ Booking not found for sagaId={}", event.getSagaId()));
    }

    /**
     * SAGA STEP 3b — Compensation path.
     * train-service could not reserve seats → mark booking FAILED.
     * No seat rollback needed here because train-service never committed them.
     */
    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.seat-reservation-failed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "seatReservationKafkaListenerContainerFactory"
    )
    public void handleSeatReservationFailed(@Payload SeatReservationEvent event) {
        log.warn("SAGA STEP 3b ▶ COMPENSATION triggered | sagaId={} | reason={}",
                event.getSagaId(), event.getFailureReason());

        bookingRepository.findBySagaId(event.getSagaId()).ifPresentOrElse(booking -> {
            booking.setStatus(Booking.BookingStatus.FAILED);
            booking.setPaymentStatus(Booking.PaymentStatus.FAILED);
            booking.setFailureReason(event.getFailureReason());
            bookingRepository.save(booking);
            log.warn("SAGA STEP 3b ✔ Booking FAILED (compensated) | sagaId={} | reason={}",
                    event.getSagaId(), event.getFailureReason());
        }, () -> log.warn("SAGA STEP 3b ✘ Booking not found for sagaId={}", event.getSagaId()));
    }

        @Transactional
        @KafkaListener(
            topics = "${app.kafka.topic.payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentResultKafkaListenerContainerFactory"
        )
        public void handlePaymentCompleted(@Payload PaymentResultEvent event) {
        log.info("SAGA STEP 4a ▶ Confirming booking after payment | sagaId={}", event.getSagaId());

        bookingRepository.findBySagaId(event.getSagaId()).ifPresentOrElse(booking -> {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            booking.setPaymentStatus(Booking.PaymentStatus.PAID);
            booking.setPaymentTransactionId(event.getPaymentTransactionId());
            booking.setFailureReason(null);
            bookingRepository.save(booking);

            bookingEventProducer.publishBookingCreated(BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sagaId(booking.getSagaId())
                .eventType("BOOKING_CREATED")
                .occurredAt(LocalDateTime.now())
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .routeId(booking.getRouteId())
                .trainId(booking.getTrainId())
                .passengerName(booking.getPassengerName())
                .passengerEmail(booking.getPassengerEmail())
                .passengerPhone(booking.getPassengerPhone())
                .seatCount(booking.getSeatCount())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .paymentMethod(booking.getPaymentMethod())
                .paymentStatus(booking.getPaymentStatus())
                .paymentReference(booking.getPaymentReference())
                .paymentTransactionId(booking.getPaymentTransactionId())
                .walletProvider(booking.getWalletProvider())
                .build());

            log.info("SAGA STEP 4a ✔ Booking CONFIRMED | sagaId={} | paymentTxn={}",
                event.getSagaId(), event.getPaymentTransactionId());
        }, () -> log.warn("SAGA STEP 4a ✘ Booking not found for sagaId={}", event.getSagaId()));
        }

        @Transactional
        @KafkaListener(
            topics = "${app.kafka.topic.payment-failed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentResultKafkaListenerContainerFactory"
        )
        public void handlePaymentFailed(@Payload PaymentResultEvent event) {
        log.warn("SAGA STEP 4b ▶ Payment failed, triggering seat compensation | sagaId={} | reason={}",
            event.getSagaId(), event.getFailureReason());

        bookingRepository.findBySagaId(event.getSagaId()).ifPresentOrElse(booking -> {
            booking.setStatus(Booking.BookingStatus.FAILED);
            booking.setPaymentStatus(Booking.PaymentStatus.FAILED);
            booking.setFailureReason(event.getFailureReason());
            bookingRepository.save(booking);

            bookingEventProducer.publishBookingFailed(BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sagaId(booking.getSagaId())
                .eventType("BOOKING_FAILED")
                .occurredAt(LocalDateTime.now())
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .routeId(booking.getRouteId())
                .trainId(booking.getTrainId())
                .passengerName(booking.getPassengerName())
                .passengerEmail(booking.getPassengerEmail())
                .passengerPhone(booking.getPassengerPhone())
                .seatCount(booking.getSeatCount())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .paymentMethod(booking.getPaymentMethod())
                .paymentStatus(booking.getPaymentStatus())
                .paymentReference(booking.getPaymentReference())
                .walletProvider(booking.getWalletProvider())
                .failureReason(event.getFailureReason())
                .build());

            log.warn("SAGA STEP 4b ✔ Booking FAILED and compensation requested | sagaId={}",
                event.getSagaId());
        }, () -> log.warn("SAGA STEP 4b ✘ Booking not found for sagaId={}", event.getSagaId()));
        }
}
