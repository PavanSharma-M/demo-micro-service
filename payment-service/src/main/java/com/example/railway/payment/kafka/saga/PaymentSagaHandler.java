package com.example.railway.payment.kafka.saga;

import com.example.railway.payment.kafka.event.PaymentMethod;
import com.example.railway.payment.kafka.event.PaymentRequestEvent;
import com.example.railway.payment.kafka.event.PaymentResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSagaHandler {

    private static final Pattern UPI_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+@[A-Za-z]+$");
    private static final Pattern CARD_PATTERN = Pattern.compile("^[0-9]{16}$");

    private final KafkaTemplate<String, PaymentResultEvent> paymentResultKafkaTemplate;

    @Value("${app.kafka.topic.payment-completed}")
    private String paymentCompletedTopic;

    @Value("${app.kafka.topic.payment-failed}")
    private String paymentFailedTopic;

    @KafkaListener(
            topics = "${app.kafka.topic.payment-requested}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentRequestKafkaListenerContainerFactory"
    )
    public void handlePaymentRequested(@Payload PaymentRequestEvent event) {
        log.info("SAGA STEP 4 ▶ Processing payment | sagaId={} | method={} | amount={}",
                event.getSagaId(), event.getPaymentMethod(), event.getAmount());

        String validationError = validate(event);
        if (validationError != null) {
            publish(paymentFailedTopic, PaymentResultEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sagaId(event.getSagaId())
                    .eventType("PAYMENT_FAILED")
                    .occurredAt(LocalDateTime.now())
                    .bookingId(event.getBookingId())
                    .bookingNumber(event.getBookingNumber())
                    .paymentMethod(event.getPaymentMethod())
                    .failureReason(validationError)
                    .build());
            return;
        }

        publish(paymentCompletedTopic, PaymentResultEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sagaId(event.getSagaId())
                .eventType("PAYMENT_COMPLETED")
                .occurredAt(LocalDateTime.now())
                .bookingId(event.getBookingId())
                .bookingNumber(event.getBookingNumber())
                .paymentMethod(event.getPaymentMethod())
                .paymentTransactionId(generatePaymentTransactionId())
                .build());
    }

    private String validate(PaymentRequestEvent event) {
        if (event.getAmount() == null || event.getAmount().signum() <= 0) {
            return "Payment amount must be greater than zero";
        }
        return switch (event.getPaymentMethod()) {
            case UPI -> UPI_PATTERN.matcher(event.getPaymentReference()).matches()
                    ? null : "Invalid UPI ID format";
            case CREDIT_CARD -> CARD_PATTERN.matcher(event.getPaymentReference()).matches()
                    ? null : "Credit card number must be 16 digits";
            case WALLET -> StringUtils.hasText(event.getWalletProvider()) && StringUtils.hasText(event.getPaymentReference())
                    ? null : "Wallet payments require provider and wallet reference";
        };
    }

    private void publish(String topic, PaymentResultEvent event) {
        paymentResultKafkaTemplate.send(topic, event.getSagaId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} sagaId={} to topic={}: {}",
                                event.getEventType(), event.getSagaId(), topic, ex.getMessage());
                    } else {
                        log.info("Published {} sagaId={} to topic={}",
                                event.getEventType(), event.getSagaId(), topic);
                    }
                });
    }

    private String generatePaymentTransactionId() {
        return "PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}