package com.example.demo_micro_service.kafka.config;

import com.example.demo_micro_service.kafka.event.BookingEvent;
import com.example.demo_micro_service.kafka.event.SeatReservationEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * KAFKA CONFIGURATION
 * ────────────────────
 * Declares two KafkaTemplate beans:
 *
 *  1. KafkaTemplate<String, BookingEvent>        — for booking lifecycle events
 *  2. KafkaTemplate<String, SeatReservationEvent> — for saga step reply events
 *
 * Spring Boot auto-configures one KafkaTemplate by default (using application.properties).
 * We declare the second one manually here so both can be injected by type in BookingEventProducer.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Shared producer settings used by both KafkaTemplates.
     * JsonSerializer converts the Java object to JSON bytes before sending.
     */
    private Map<String, Object> baseProducerProps(Class<?> valueSerializer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        // Include the full class name in the JSON message so the consumer knows the type
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return props;
    }

    /**
     * Bean 1: KafkaTemplate for BookingEvent.
     * Spring Boot's auto-configured template also covers this type, but we declare it
     * explicitly so the injection in BookingEventProducer is unambiguous.
     */
    @Bean
    public KafkaTemplate<String, BookingEvent> bookingEventKafkaTemplate() {
        ProducerFactory<String, BookingEvent> factory =
                new DefaultKafkaProducerFactory<>(baseProducerProps(JsonSerializer.class));
        return new KafkaTemplate<>(factory);
    }

    /**
     * Bean 2: KafkaTemplate for SeatReservationEvent.
     * A separate template is needed because KafkaTemplate is typed — one instance
     * is bound to one value serializer configuration at creation time.
     */
    @Bean
    public KafkaTemplate<String, SeatReservationEvent> seatReservationKafkaTemplate() {
        ProducerFactory<String, SeatReservationEvent> factory =
                new DefaultKafkaProducerFactory<>(baseProducerProps(JsonSerializer.class));
        return new KafkaTemplate<>(factory);
    }
}
