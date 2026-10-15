package com.example.railway.booking.kafka.producer;

import com.example.railway.booking.kafka.event.BookingEvent;
import com.example.railway.booking.kafka.event.PaymentRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes booking events to Kafka topics.
 *
 * Topics:
 *   booking-initiated  → consumed by train-service (saga step 2: reserve seats)
 *   booking-created    → notification (e.g. email service)
 *   booking-cancelled  → notification
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, BookingEvent> bookingEventKafkaTemplate;
    private final KafkaTemplate<String, PaymentRequestEvent> paymentRequestKafkaTemplate;

    @Value("${app.kafka.topic.booking-initiated}")
    private String bookingInitiatedTopic;

    @Value("${app.kafka.topic.booking-created}")
    private String bookingCreatedTopic;

    @Value("${app.kafka.topic.booking-cancelled}")
    private String bookingCancelledTopic;

    @Value("${app.kafka.topic.booking-failed}")
    private String bookingFailedTopic;

    @Value("${app.kafka.topic.payment-requested}")
    private String paymentRequestedTopic;

    /**
     * SAGA STEP 1: Publishes booking-initiated so train-service can reserve seats.
     * Booking is saved as PENDING until train-service replies.
     */
    public void publishBookingInitiated(BookingEvent event) {
        send(bookingInitiatedTopic, event);
    }

    /** Notification event after booking is fully CONFIRMED. */
    public void publishBookingCreated(BookingEvent event) {
        send(bookingCreatedTopic, event);
    }

    /** Notification event after booking is CANCELLED. */
    public void publishBookingCancelled(BookingEvent event) {
        send(bookingCancelledTopic, event);
    }

    public void publishBookingFailed(BookingEvent event) {
        send(bookingFailedTopic, event);
    }

    public void publishPaymentRequested(PaymentRequestEvent event) {
        paymentRequestKafkaTemplate.send(paymentRequestedTopic, event.getSagaId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment event type={} sagaId={} to topic={}: {}",
                                event.getEventType(), event.getSagaId(), paymentRequestedTopic, ex.getMessage());
                    } else {
                        log.info("Published payment event type={} sagaId={} to topic={} partition={} offset={}",
                                event.getEventType(), event.getSagaId(), paymentRequestedTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    private void send(String topic, BookingEvent event) {
        bookingEventKafkaTemplate.send(topic, event.getSagaId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event type={} sagaId={} to topic={}: {}",
                                event.getEventType(), event.getSagaId(), topic, ex.getMessage());
                    } else {
                        log.info("Published event type={} sagaId={} to topic={} partition={} offset={}",
                                event.getEventType(), event.getSagaId(), topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
