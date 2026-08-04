package com.example.demo_micro_service.kafka.saga;

import com.example.demo_micro_service.kafka.event.BookingEvent;
import com.example.demo_micro_service.kafka.event.SeatReservationEvent;
import com.example.demo_micro_service.kafka.producer.BookingEventProducer;
import com.example.demo_micro_service.model.Train;
import com.example.demo_micro_service.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║           SAGA STEP 2 — Seat Reservation Handler                ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Choreography Saga — Step 2 of 3
 *
 * Listens to: "booking-initiated"  (published by BookingService in Step 1)
 * Publishes:  "seat-reserved"      (saga proceeds → Step 3a)
 *         or: "seat-reservation-failed" (compensation → Step 3b)
 *
 * ─── WHAT IS A CHOREOGRAPHY SAGA? ───────────────────────────────────
 * In a Choreography Saga, there is NO central controller.
 * Each participant (handler) listens to events and reacts by either:
 *   • doing its local work and publishing a SUCCESS event, OR
 *   • publishing a FAILURE event to trigger compensation (rollback).
 *
 * Each step has TWO transactions:
 *   1. LOCAL DB transaction  — do the actual work (deduct seats)
 *   2. Kafka publish         — tell the next participant what happened
 *
 * If the DB transaction succeeds but Kafka fails → retry / dead-letter.
 * If Kafka arrives but DB fails → the compensation chain fires.
 *
 * ─── THIS HANDLER'S JOB ──────────────────────────────────────────────
 *   Receives: "booking-initiated" event  (sagaId + routeId + seatCount + trainId)
 *   Does:     Tries to reserve seats in the Train table
 *   Replies:  "seat-reserved" if OK  |  "seat-reservation-failed" if not
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatReservationSagaHandler {

    private final TrainRepository trainRepository;
    private final BookingEventProducer producer;

    /**
     * SAGA STEP 2: Reserve seats.
     *
     * @Transactional ensures the seat deduction is atomic.
     * If this method throws, the DB rolls back and we publish a FAILED event.
     */
    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.booking-initiated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleBookingInitiated(@Payload BookingEvent event) {
        log.info("SAGA STEP 2 ▶ SeatReservation | sagaId={} | trainId={} | seats={}",
                event.getSagaId(), event.getTrainId(), event.getSeatCount());

        try {
            Train train = trainRepository.findById(event.getTrainId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Train not found: " + event.getTrainId()));

            // ── CHECK: are there enough seats? ────────────────────────
            if (train.getAvailableSeats() < event.getSeatCount()) {
                publishFailure(event, "Not enough seats available. Available: "
                        + train.getAvailableSeats() + ", requested: " + event.getSeatCount());
                return;
            }

            // ── LOCAL TRANSACTION: deduct seats ───────────────────────
            train.setAvailableSeats(train.getAvailableSeats() - event.getSeatCount());
            trainRepository.save(train);

            log.info("SAGA STEP 2 ✔ Seats reserved | sagaId={} | remaining seats={}",
                    event.getSagaId(), train.getAvailableSeats());

            // ── PUBLISH SUCCESS → triggers Step 3 ─────────────────────
            producer.publishSeatReserved(SeatReservationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sagaId(event.getSagaId())
                    .eventType("SEAT_RESERVED")
                    .trainId(train.getId())
                    .seatCount(event.getSeatCount())
                    .build());

        } catch (Exception ex) {
            log.error("SAGA STEP 2 ✘ Seat reservation failed | sagaId={} | reason={}",
                    event.getSagaId(), ex.getMessage(), ex);
            publishFailure(event, ex.getMessage());
        }
    }

    /**
     * Helper: publish SEAT_RESERVATION_FAILED event.
     * This is the COMPENSATION TRIGGER — the next handler will mark the booking FAILED.
     */
    private void publishFailure(BookingEvent event, String reason) {
        producer.publishSeatReservationFailed(SeatReservationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sagaId(event.getSagaId())
                .eventType("SEAT_RESERVATION_FAILED")
                .trainId(event.getTrainId())
                .seatCount(event.getSeatCount())
                .failureReason(reason)
                .build());
    }
}
