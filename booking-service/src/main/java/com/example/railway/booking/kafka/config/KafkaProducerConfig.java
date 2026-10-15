package com.example.railway.booking.kafka.config;

import com.example.railway.booking.kafka.event.BookingEvent;
import com.example.railway.booking.kafka.event.PaymentRequestEvent;
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
 * Kafka producer configuration for booking-service.
 * Declares KafkaTemplate beans for booking lifecycle and payment request events.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> producerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return props;
    }

    @Bean
    public KafkaTemplate<String, BookingEvent> bookingEventKafkaTemplate() {
        ProducerFactory<String, BookingEvent> factory =
                new DefaultKafkaProducerFactory<>(producerProps());
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public KafkaTemplate<String, PaymentRequestEvent> paymentRequestKafkaTemplate() {
        ProducerFactory<String, PaymentRequestEvent> factory =
                new DefaultKafkaProducerFactory<>(producerProps());
        return new KafkaTemplate<>(factory);
    }
}
