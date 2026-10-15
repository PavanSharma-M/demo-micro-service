package com.example.railway.booking.service;

import com.example.railway.booking.client.RouteClient;
import com.example.railway.booking.client.TrainClient;
import com.example.railway.booking.dto.BookingRequestDTO;
import com.example.railway.booking.dto.BookingResponseDTO;
import com.example.railway.booking.dto.RouteDTO;
import com.example.railway.booking.exception.BookingException;
import com.example.railway.booking.exception.ResourceNotFoundException;
import com.example.railway.booking.kafka.event.BookingEvent;
import com.example.railway.booking.kafka.producer.BookingEventProducer;
import com.example.railway.booking.model.Booking;
import com.example.railway.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RouteClient routeClient;
    private final TrainClient trainClient;
    private final BookingEventProducer bookingEventProducer;

    /**
     * SAGA STEP 1 — Create booking as PENDING and publish booking-initiated event.
     *
     * The booking is saved immediately as PENDING (not yet confirmed).
     * train-service reserves seats first; payment-service processes payment second.
     * BookingSagaCompletionHandler finalizes confirmation or compensation.
     */
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        log.info("Creating booking for routeId={} seats={} passenger={}",
                request.getRouteId(), request.getSeatCount(), request.getPassengerEmail());

        RouteDTO route = routeClient.getRouteById(request.getRouteId());
        log.debug("Fetched route: train={} availableSeats={}", route.getTrainName(), route.getAvailableSeats());

        String bookingNumber = generateBookingNumber();
        String sagaId = bookingNumber; // sagaId = bookingNumber as correlation ID

        Booking booking = Booking.builder()
                .bookingNumber(bookingNumber)
                .sagaId(sagaId)
                .routeId(request.getRouteId())
            .trainId(route.getTrainId())
                .passengerName(request.getPassengerName())
                .passengerEmail(request.getPassengerEmail())
                .passengerPhone(request.getPassengerPhone())
                .seatCount(request.getSeatCount())
                .bookingDate(LocalDateTime.now())
                .status(Booking.BookingStatus.PENDING)
            .paymentMethod(request.getPaymentMethod())
            .paymentStatus(Booking.PaymentStatus.PENDING)
            .walletProvider(request.getWalletProvider())
                .paymentReference(request.getPaymentReference())
                .totalAmount(route.getFare().multiply(java.math.BigDecimal.valueOf(request.getSeatCount())))
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking saved as PENDING: bookingNumber={}", bookingNumber);

        // Publish saga step 1 — train-service will consume this
        bookingEventProducer.publishBookingInitiated(BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sagaId(sagaId)
                .eventType("BOOKING_INITIATED")
                .occurredAt(LocalDateTime.now())
                .bookingId(saved.getId())
                .bookingNumber(bookingNumber)
                .routeId(request.getRouteId())
                .trainId(route.getTrainId())
                .trainName(route.getTrainName())
                .trainNumber(route.getTrainNumber())
                .sourceStation(route.getSourceStationName())
                .destinationStation(route.getDestinationStationName())
                .departureTime(route.getDepartureTime())
                .passengerName(request.getPassengerName())
                .passengerEmail(request.getPassengerEmail())
                .passengerPhone(request.getPassengerPhone())
                .seatCount(request.getSeatCount())
                .totalAmount(saved.getTotalAmount())
                .status(Booking.BookingStatus.PENDING)
                .paymentMethod(saved.getPaymentMethod())
                .paymentStatus(saved.getPaymentStatus())
                .paymentReference(request.getPaymentReference())
                .walletProvider(saved.getWalletProvider())
                .build());

        return toResponseDTO(saved, route);
    }

    public BookingResponseDTO getBookingById(Long id) {
        log.debug("Fetching booking id={}", id);
        Booking booking = findById(id);
        RouteDTO route = routeClient.getRouteById(booking.getRouteId());
        return toResponseDTO(booking, route);
    }

    public BookingResponseDTO getBookingByNumber(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with number: " + bookingNumber));
        RouteDTO route = routeClient.getRouteById(booking.getRouteId());
        return toResponseDTO(booking, route);
    }

    public List<BookingResponseDTO> getBookingsByEmail(String email) {
        return bookingRepository.findByPassengerEmail(email).stream()
                .map(b -> toResponseDTO(b, routeClient.getRouteById(b.getRouteId())))
                .toList();
    }

    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(b -> toResponseDTO(b, routeClient.getRouteById(b.getRouteId())))
                .toList();
    }

    public BookingResponseDTO cancelBooking(Long id) {
        log.info("Cancelling booking id={}", id);
        Booking booking = findById(id);
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            log.warn("Booking id={} is already cancelled", id);
            throw new BookingException("Booking is already cancelled");
        }

        RouteDTO route = routeClient.getRouteById(booking.getRouteId());
        // Release seats synchronously via Feign (cancel is immediate)
        trainClient.updateSeats(route.getTrainId(), booking.getSeatCount());
        log.debug("Released {} seats on trainId={}", booking.getSeatCount(), route.getTrainId());

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        bookingEventProducer.publishBookingCancelled(BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sagaId(booking.getSagaId())
                .eventType("BOOKING_CANCELLED")
                .occurredAt(LocalDateTime.now())
                .bookingId(saved.getId())
                .bookingNumber(saved.getBookingNumber())
                .routeId(saved.getRouteId())
                .trainId(saved.getTrainId())
                .passengerName(saved.getPassengerName())
                .passengerEmail(saved.getPassengerEmail())
                .seatCount(saved.getSeatCount())
                .totalAmount(saved.getTotalAmount())
                .status(Booking.BookingStatus.CANCELLED)
                .paymentMethod(saved.getPaymentMethod())
                .paymentStatus(saved.getPaymentStatus())
                .paymentReference(saved.getPaymentReference())
                .walletProvider(saved.getWalletProvider())
                .paymentTransactionId(saved.getPaymentTransactionId())
                .build());

        log.info("Booking cancelled: bookingNumber={}", saved.getBookingNumber());
        return toResponseDTO(saved, route);
    }

    private Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private String generateBookingNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "BK" + timestamp + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String maskPaymentReference(Booking.PaymentMethod paymentMethod, String paymentReference) {
        if (paymentReference == null || paymentReference.length() < 4) {
            return paymentReference;
        }
        if (paymentMethod == Booking.PaymentMethod.CREDIT_CARD) {
            return "****-****-****-" + paymentReference.substring(paymentReference.length() - 4);
        }
        int visible = Math.min(4, paymentReference.length());
        return "****" + paymentReference.substring(paymentReference.length() - visible);
    }

    private BookingResponseDTO toResponseDTO(Booking booking, RouteDTO route) {
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .routeId(booking.getRouteId())
                .trainName(route.getTrainName())
                .trainNumber(route.getTrainNumber())
                .sourceStation(route.getSourceStationName() + " (" + route.getSourceStationCode() + ")")
                .destinationStation(route.getDestinationStationName() + " (" + route.getDestinationStationCode() + ")")
                .departureTime(route.getDepartureTime())
                .arrivalTime(route.getArrivalTime())
                .passengerName(booking.getPassengerName())
                .passengerEmail(booking.getPassengerEmail())
                .passengerPhone(booking.getPassengerPhone())
                .seatCount(booking.getSeatCount())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .paymentMethod(booking.getPaymentMethod())
                .paymentStatus(booking.getPaymentStatus())
                .paymentReference(maskPaymentReference(booking.getPaymentMethod(), booking.getPaymentReference()))
                .paymentTransactionId(booking.getPaymentTransactionId())
                .totalAmount(booking.getTotalAmount())
                .failureReason(booking.getFailureReason())
                .build();
    }
}
