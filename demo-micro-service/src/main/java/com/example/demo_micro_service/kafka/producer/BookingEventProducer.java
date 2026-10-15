package com.example.demo_micro_service.kafka.producer;

import com.example.demo_micro_service.kafka.event.BookingEvent;
import com.example.demo_micro_service.kafka.event.SeatReservationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * KAFKA PRODUCER
 * ──────────────
 * Responsible for SENDING (publishing) events to Kafka topics.
 *
 * KafkaTemplate is the Spring wrapper around the low-level Kafka Producer API.
 * It handles:
 *   - JSON serialization of BookingEvent / SeatReservationEvent
 *   - Routing the message to the correct topic
 *   - Async acknowledgment from the Kafka broker
 *
 * Think of this as a "post office": you hand it a letter (event)
 * and it delivers it to the correct mailbox (topic).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventProducer {

    // KafkaTemplate for BookingEvent payloads
    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    // KafkaTemplate for SeatReservationEvent payloads (saga step replies)
    private final KafkaTemplate<String, SeatReservationEvent> seatKafkaTemplate;

    // ─── Standard notification topics ────────────────────────────────
    @Value("${app.kafka.topic.booking-created}")
    private String bookingCreatedTopic;

    @Value("${app.kafka.topic.booking-cancelled}")
    private String bookingCancelledTopic;

    // ─── Saga topics ──────────────────────────────────────────────────
    @Value("${app.kafka.topic.booking-initiated}")
    private String bookingInitiatedTopic;

    @Value("${app.kafka.topic.seat-reserved}")
    private String seatReservedTopic;

    @Value("${app.kafka.topic.seat-reservation-failed}")
    private String seatReservationFailedTopic;

    @Value("${app.kafka.topic.booking-confirmed}")
    private String bookingConfirmedTopic;

    @Value("${app.kafka.topic.booking-failed}")
    private String bookingFailedTopic;

    // ─── Standard notification publishers ────────────────────────────

    public void publishBookingCreated(BookingEvent event) {
        sendBookingEvent(bookingCreatedTopic, event);
    }

    public void publishBookingCancelled(BookingEvent event) {
        sendBookingEvent(bookingCancelledTopic, event);
    }

    // ─── Saga publishers ──────────────────────────────────────────────

    /** SAGA STEP 1 → 2 : BookingService starts the saga. */
    public void publishBookingInitiated(BookingEvent event) {
        sendBookingEvent(bookingInitiatedTopic, event);
    }

    /** SAGA STEP 2 → 3 (success) : SeatReservationSagaHandler confirms seats reserved. */
    public void publishSeatReserved(SeatReservationEvent event) {
        sendSeatEvent(seatReservedTopic, event);
    }

    /** SAGA STEP 2 → 3 (failure) : SeatReservationSagaHandler triggers compensation. */
    public void publishSeatReservationFailed(SeatReservationEvent event) {
        sendSeatEvent(seatReservationFailedTopic, event);
    }

    /** SAGA STEP 3 (success) : BookingSagaCompletionHandler finalizes with CONFIRMED. */
    public void publishBookingConfirmed(BookingEvent event) {
        sendBookingEvent(bookingConfirmedTopic, event);
    }

    /** SAGA STEP 3 (compensation) : BookingSagaCompletionHandler finalizes with FAILED. */
    public void publishBookingFailed(BookingEvent event) {
        sendBookingEvent(bookingFailedTopic, event);
    }

    // ─── Internal helpers ─────────────────────────────────────────────

    private void sendBookingEvent(String topic, BookingEvent event) {
        CompletableFuture<SendResult<String, BookingEvent>> future =
                kafkaTemplate.send(topic, event.getSagaId(), event);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published [{}] sagaId={} → topic={}, partition={}, offset={}",
                        event.getEventType(), event.getSagaId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish [{}] sagaId={}: {}",
                        event.getEventType(), event.getSagaId(), ex.getMessage(), ex);
            }
        });
    }

    private void sendSeatEvent(String topic, SeatReservationEvent event) {
        CompletableFuture<SendResult<String, SeatReservationEvent>> future =
                seatKafkaTemplate.send(topic, event.getSagaId(), event);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published [{}] sagaId={} → topic={}, partition={}, offset={}",
                        event.getEventType(), event.getSagaId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish [{}] sagaId={}: {}",
                        event.getEventType(), event.getSagaId(), ex.getMessage(), ex);
            }
        });
    }
}
