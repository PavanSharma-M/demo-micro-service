package com.example.demo_micro_service.kafka.consumer;

import com.example.demo_micro_service.kafka.event.BookingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * KAFKA CONSUMER
 * ──────────────
 * Responsible for RECEIVING (consuming) events from Kafka topics.
 *
 * @KafkaListener registers this method as a subscriber to a topic.
 * Spring Kafka polls the Kafka broker in a background thread and
 * calls the annotated method each time a new message arrives.
 *
 * Think of this as a "mailman": it continuously checks the mailbox
 * (topic) and processes each letter (event) it finds.
 *
 * In this demo, the consumer and producer live in the same app.
 * In a real microservice architecture, you would split them:
 *   • booking-service   → Producer only
 *   • notification-service → Consumer only (sends emails, SMS)
 *   • analytics-service    → Consumer only (records stats)
 */
@Slf4j
@Component
public class BookingEventConsumer {

    /**
     * Listens to the "booking-created" topic.
     *
     * @Payload      — deserializes the JSON message body into BookingEvent
     * @Header(...)  — extracts Kafka metadata (partition, offset) for logging
     *
     * groupId = "railway-booking-group":
     *   All consumers sharing the same groupId act as a LOAD-BALANCED team.
     *   Each message is delivered to exactly ONE consumer in the group.
     *   If you run 2 instances of this app, Kafka will split the partitions
     *   between them — each instance only sees half the messages.
     */
    @KafkaListener(
            topics = "${app.kafka.topic.booking-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleBookingCreated(
            @Payload BookingEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("──────────────────────────────────────────");
        log.info("RECEIVED [BOOKING_CREATED] event");
        log.info("  Booking  : {} (ID: {})", event.getBookingNumber(), event.getBookingId());
        log.info("  Passenger: {} <{}>", event.getPassengerName(), event.getPassengerEmail());
        log.info("  Route    : {} → {}", event.getSourceStation(), event.getDestinationStation());
        log.info("  Train    : {} ({})", event.getTrainName(), event.getTrainNumber());
        log.info("  Seats    : {} | Amount: ₹{}", event.getSeatCount(), event.getTotalAmount());
        log.info("  Departure: {}", event.getDepartureTime());
        log.info("  Kafka    : partition={}, offset={}", partition, offset);
        log.info("──────────────────────────────────────────");

        // ──────────────────────────────────────────────────────────────
        // EXTENSION POINT: Add real business actions here, for example:
        //
        //   emailService.sendBookingConfirmation(event);
        //   smsService.sendSms(event.getPassengerPhone(), "Your booking is confirmed!");
        //   analyticsService.trackBooking(event);
        //   invoiceService.generateInvoice(event);
        // ──────────────────────────────────────────────────────────────
    }

    /**
     * Listens to the "booking-cancelled" topic.
     *
     * Separate listener = separate handler logic per event type.
     * Both share the same consumer group so Kafka balances load evenly.
     */
    @KafkaListener(
            topics = "${app.kafka.topic.booking-cancelled}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleBookingCancelled(
            @Payload BookingEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("──────────────────────────────────────────");
        log.info("RECEIVED [BOOKING_CANCELLED] event");
        log.info("  Booking  : {} (ID: {})", event.getBookingNumber(), event.getBookingId());
        log.info("  Passenger: {} <{}>", event.getPassengerName(), event.getPassengerEmail());
        log.info("  Kafka    : partition={}, offset={}", partition, offset);
        log.info("──────────────────────────────────────────");

        // ──────────────────────────────────────────────────────────────
        // EXTENSION POINT:
        //
        //   emailService.sendCancellationConfirmation(event);
        //   refundService.initiateRefund(event.getBookingNumber());
        // ──────────────────────────────────────────────────────────────
    }
}
