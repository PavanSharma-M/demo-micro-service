package com.example.railway.train.kafka.saga;

import com.example.railway.train.kafka.event.BookingEvent;
import com.example.railway.train.kafka.event.SeatReservationEvent;
import com.example.railway.train.model.Train;
import com.example.railway.train.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║   SAGA STEP 2 — Seat Reservation Handler                        ║
 * ║   Lives in: train-service                                       ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Listens to: "booking-initiated"      (published by booking-service step 1)
 * Publishes:  "seat-reserved"          (success → step 3a in booking-service)
 *         or: "seat-reservation-failed" (failure → step 3b compensation)
 *
 * This replaces the synchronous Feign call that booking-service previously
 * made to train-service. The service now operates asynchronously:
 *   booking-service saves PENDING booking → publishes event
 *   train-service reserves seats → publishes reply
 *   booking-service confirms or compensates
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatReservationSagaHandler {

    private final TrainRepository trainRepository;
    private final KafkaTemplate<String, SeatReservationEvent> seatReservationKafkaTemplate;

    @Value("${app.kafka.topic.seat-reserved}")
    private String seatReservedTopic;

    @Value("${app.kafka.topic.seat-reservation-failed}")
    private String seatReservationFailedTopic;

    /**
     * SAGA STEP 2: Receive booking-initiated → reserve seats → publish reply.
     *
     * @Transactional ensures the seat deduction is atomic.
     * If DB commit succeeds but Kafka publish fails, the event is lost
     * (for production, use Kafka transactions or outbox pattern).
     */
    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.booking-initiated}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "bookingEventKafkaListenerContainerFactory"
    )
    public void handleBookingInitiated(@Payload BookingEvent event) {
        log.info("SAGA STEP 2 ▶ SeatReservation | sagaId={} | trainId={} | seats={}",
                event.getSagaId(), event.getTrainId(), event.getSeatCount());

        try {
            Train train = trainRepository.findById(event.getTrainId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Train not found: " + event.getTrainId()));

            if (train.getAvailableSeats() < event.getSeatCount()) {
                String reason = "Not enough seats. Available: "
                        + train.getAvailableSeats() + ", requested: " + event.getSeatCount();
                log.warn("SAGA STEP 2 ✘ {} | sagaId={}", reason, event.getSagaId());
                publishReply(seatReservationFailedTopic, SeatReservationEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .sagaId(event.getSagaId())
                        .eventType("SEAT_RESERVATION_FAILED")
                        .trainId(event.getTrainId())
                        .seatCount(event.getSeatCount())
                        .failureReason(reason)
                        .build());
                return;
            }

            // LOCAL TRANSACTION: deduct seats
            train.setAvailableSeats(train.getAvailableSeats() - event.getSeatCount());
            trainRepository.save(train);

            log.info("SAGA STEP 2 ✔ Seats reserved | sagaId={} | remainingSeats={}",
                    event.getSagaId(), train.getAvailableSeats());

            publishReply(seatReservedTopic, SeatReservationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sagaId(event.getSagaId())
                    .eventType("SEAT_RESERVED")
                    .trainId(train.getId())
                    .seatCount(event.getSeatCount())
                    .build());

        } catch (Exception ex) {
            log.error("SAGA STEP 2 ✘ Unexpected error | sagaId={} | reason={}",
                    event.getSagaId(), ex.getMessage(), ex);
            publishReply(seatReservationFailedTopic, SeatReservationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sagaId(event.getSagaId())
                    .eventType("SEAT_RESERVATION_FAILED")
                    .trainId(event.getTrainId())
                    .seatCount(event.getSeatCount())
                    .failureReason(ex.getMessage())
                    .build());
        }
    }

        @Transactional
        @KafkaListener(
                        topics = "${app.kafka.topic.booking-failed}",
                        groupId = "${spring.kafka.consumer.group-id}",
                        containerFactory = "bookingEventKafkaListenerContainerFactory"
        )
        public void handleBookingFailedCompensation(@Payload BookingEvent event) {
                log.warn("SAGA COMPENSATION ▶ Releasing seats after payment failure | sagaId={} | trainId={} | seats={}",
                                event.getSagaId(), event.getTrainId(), event.getSeatCount());

                trainRepository.findById(event.getTrainId()).ifPresentOrElse(train -> {
                        int restored = Math.min(train.getTotalSeats(), train.getAvailableSeats() + event.getSeatCount());
                        train.setAvailableSeats(restored);
                        trainRepository.save(train);
                        log.info("SAGA COMPENSATION ✔ Seats released | sagaId={} | availableSeats={}",
                                        event.getSagaId(), restored);
                }, () -> log.warn("SAGA COMPENSATION ✘ Train not found for trainId={}", event.getTrainId()));
        }

    private void publishReply(String topic, SeatReservationEvent event) {
        seatReservationKafkaTemplate.send(topic, event.getSagaId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} to topic={}: {}",
                                event.getEventType(), topic, ex.getMessage());
                    } else {
                        log.info("Published {} sagaId={} to topic={}",
                                event.getEventType(), event.getSagaId(), topic);
                    }
                });
    }
}
