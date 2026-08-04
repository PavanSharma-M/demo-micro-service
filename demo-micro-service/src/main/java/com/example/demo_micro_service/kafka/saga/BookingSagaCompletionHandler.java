package com.example.demo_micro_service.kafka.saga;

import com.example.demo_micro_service.kafka.event.BookingEvent;
import com.example.demo_micro_service.kafka.event.SeatReservationEvent;
import com.example.demo_micro_service.kafka.producer.BookingEventProducer;
import com.example.demo_micro_service.model.Booking;
import com.example.demo_micro_service.repository.BookingRepository;
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
 * ║        SAGA STEP 3 — Booking Saga Completion Handler            ║
 * ║        (handles BOTH the happy path AND the compensation)       ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Choreography Saga — Step 3 of 3
 *
 * ─── HAPPY PATH (Step 3a) ────────────────────────────────────────────
 *   Listens to: "seat-reserved"    (seats were successfully deducted)
 *   Does:       Update Booking → status = CONFIRMED
 *   Publishes:  "booking-confirmed" (final notification event)
 *
 * ─── COMPENSATION (Step 3b) ─────────────────────────────────────────
 *   Listens to: "seat-reservation-failed"   (seats could not be reserved)
 *   Does:       Update Booking → status = FAILED, stores failure reason
 *   Publishes:  "booking-failed" (notification event for monitoring/email)
 *
 * ─── WHAT IS COMPENSATION? ──────────────────────────────────────────
 * In a distributed transaction, if one step fails AFTER previous steps
 * already committed, you cannot call rollback() across services.
 * Instead, you execute a COMPENSATING TRANSACTION:
 *   • Undo the effects of any steps that already succeeded.
 *   • Here: if seats were already deducted before failure, restore them.
 *   • The booking is marked FAILED (not deleted) so the audit trail is preserved.
 *
 * The sagaId (= bookingNumber) is the CORRELATION ID that lets this handler
 * find the correct Booking record in the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSagaCompletionHandler {

    private final BookingRepository bookingRepository;
    private final BookingEventProducer producer;

    // ─── STEP 3a: Happy Path ──────────────────────────────────────────

    /**
     * Receives "seat-reserved" → finalizes the booking as CONFIRMED.
     *
     * The sagaId in the event matches the Booking.sagaId in the DB,
     * so we can look up and update the correct record.
     */
    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.seat-reserved}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleSeatReserved(@Payload SeatReservationEvent event) {
        log.info("SAGA STEP 3a ▶ Confirming booking | sagaId={}", event.getSagaId());

        Booking booking = findBySagaId(event.getSagaId());
        if (booking == null) return;

        // ── LOCAL TRANSACTION: mark booking CONFIRMED ─────────────────
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        log.info("SAGA STEP 3a ✔ Booking CONFIRMED | sagaId={} | bookingId={}",
                event.getSagaId(), saved.getId());

        // ── Publish final notification ─────────────────────────────────
        producer.publishBookingConfirmed(toBookingEvent(saved, "BOOKING_CONFIRMED"));
    }

    // ─── STEP 3b: Compensation Path ───────────────────────────────────

    /**
     * Receives "seat-reservation-failed" → COMPENSATION TRANSACTION.
     *
     * The booking was saved as PENDING in Step 1.
     * Step 2 failed (no seats). Now we must undo Step 1:
     *   → Mark booking as FAILED with the failure reason.
     *   → No seat rollback needed here because Step 2 never actually deducted them.
     *
     * If Step 2 had partially succeeded (seats deducted but Kafka failed),
     * a dead-letter queue / retry mechanism would ensure this handler still fires.
     */
    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.seat-reservation-failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleSeatReservationFailed(@Payload SeatReservationEvent event) {
        log.warn("SAGA STEP 3b ▶ COMPENSATION triggered | sagaId={} | reason={}",
                event.getSagaId(), event.getFailureReason());

        Booking booking = findBySagaId(event.getSagaId());
        if (booking == null) return;

        // ── COMPENSATION: mark booking FAILED ─────────────────────────
        booking.setStatus(Booking.BookingStatus.FAILED);
        booking.setFailureReason(event.getFailureReason());
        Booking saved = bookingRepository.save(booking);

        log.warn("SAGA STEP 3b ✔ Booking FAILED (compensated) | sagaId={} | reason={}",
                event.getSagaId(), event.getFailureReason());

        // ── Publish failure notification ───────────────────────────────
        producer.publishBookingFailed(toBookingEvent(saved, "BOOKING_FAILED"));
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private Booking findBySagaId(String sagaId) {
        return bookingRepository.findBySagaId(sagaId)
                .orElseGet(() -> {
                    log.error("SAGA ERROR: No booking found for sagaId={}. Possible duplicate or lost event.", sagaId);
                    return null;
                });
    }

    private BookingEvent toBookingEvent(Booking booking, String eventType) {
        return BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sagaId(booking.getSagaId())
                .eventType(eventType)
                .occurredAt(LocalDateTime.now())
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .passengerName(booking.getPassengerName())
                .passengerEmail(booking.getPassengerEmail())
                .passengerPhone(booking.getPassengerPhone())
                .seatCount(booking.getSeatCount())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .build();
    }
}
