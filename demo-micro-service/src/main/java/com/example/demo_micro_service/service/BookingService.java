package com.example.demo_micro_service.service;

import com.example.demo_micro_service.dto.BookingRequestDTO;
import com.example.demo_micro_service.dto.BookingResponseDTO;
import com.example.demo_micro_service.exception.BookingException;
import com.example.demo_micro_service.exception.ResourceNotFoundException;
import com.example.demo_micro_service.kafka.event.BookingEvent;
import com.example.demo_micro_service.kafka.producer.BookingEventProducer;
import com.example.demo_micro_service.model.Booking;
import com.example.demo_micro_service.model.Route;
import com.example.demo_micro_service.model.Train;
import com.example.demo_micro_service.repository.BookingRepository;
import com.example.demo_micro_service.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RouteService routeService;
    private final TrainRepository trainRepository;
    private final BookingEventProducer bookingEventProducer;

    /**
     * ╔══════════════════════════════════════════════════════════════════╗
     * ║           SAGA STEP 1 — Initiate Booking Saga                   ║
     * ╚══════════════════════════════════════════════════════════════════╝
     *
     * Old approach (NOT saga):
     *   1. Check seats → deduct seats → save CONFIRMED → return 201
     *   ✗ Problem: if DB save fails after seat deduction, seats are lost.
     *   ✗ Problem: no way to retry/compensate individual steps.
     *
     * New approach (Choreography Saga):
     *   Step 1 (here): Save booking as PENDING → publish "booking-initiated" → return 202
     *   Step 2 (SeatReservationSagaHandler): deduct seats → publish "seat-reserved" or "seat-reservation-failed"
     *   Step 3a (BookingSagaCompletionHandler): on success → mark CONFIRMED → publish "booking-confirmed"
     *   Step 3b (BookingSagaCompletionHandler): on failure → COMPENSATION → mark FAILED → publish "booking-failed"
     *
     * Returns 202 Accepted (PENDING) because confirmation is async.
     * Client polls GET /api/v1/bookings/{id} to check status.
     */
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        Route route = routeService.findById(request.getRouteId());
        String bookingNumber = generateBookingNumber();

        // ── STEP 1a: Save booking as PENDING ─────────────────────────────
        // No seat deduction yet. The saga handles that in Step 2.
        // sagaId = bookingNumber — used as the correlation ID across all Kafka events.
        Booking booking = Booking.builder()
                .bookingNumber(bookingNumber)
                .sagaId(bookingNumber)                      // ← correlation ID
                .route(route)
                .passengerName(request.getPassengerName())
                .passengerEmail(request.getPassengerEmail())
                .passengerPhone(request.getPassengerPhone())
                .seatCount(request.getSeatCount())
                .bookingDate(LocalDateTime.now())
                .status(Booking.BookingStatus.PENDING)      // ← NOT confirmed yet
                .totalAmount(route.getFare().multiply(java.math.BigDecimal.valueOf(request.getSeatCount())))
                .build();

        BookingResponseDTO response = toResponseDTO(bookingRepository.save(booking));

        // ── STEP 1b: Publish "booking-initiated" → starts the saga ────────
        // SeatReservationSagaHandler will pick this up and attempt seat reservation.
        bookingEventProducer.publishBookingInitiated(
                toBookingEvent(response, "BOOKING_INITIATED", route.getTrain().getId()));

        return response; // status = PENDING  (HTTP 202 Accepted from controller)
    }

    public BookingResponseDTO getBookingById(Long id) {
        return toResponseDTO(findById(id));
    }

    public BookingResponseDTO getBookingByNumber(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with number: " + bookingNumber));
        return toResponseDTO(booking);
    }

    public List<BookingResponseDTO> getBookingsByEmail(String email) {
        return bookingRepository.findByPassengerEmail(email).stream().map(this::toResponseDTO).toList();
    }

    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    public BookingResponseDTO cancelBooking(Long id) {
        Booking booking = findById(id);

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }
        if (booking.getStatus() == Booking.BookingStatus.PENDING) {
            throw new BookingException("Cannot cancel a booking that is still PENDING. Wait for confirmation first.");
        }
        if (booking.getStatus() == Booking.BookingStatus.FAILED) {
            throw new BookingException("Cannot cancel a FAILED booking.");
        }

        // Restore available seats
        Train train = booking.getRoute().getTrain();
        train.setAvailableSeats(train.getAvailableSeats() + booking.getSeatCount());
        trainRepository.save(train);

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        BookingResponseDTO response = toResponseDTO(bookingRepository.save(booking));

        // Publish BOOKING_CANCELLED notification event
        bookingEventProducer.publishBookingCancelled(
                toBookingEvent(response, "BOOKING_CANCELLED", train.getId()));

        return response;
    }

    private Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private String generateBookingNumber() {
        return "RLY-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Converts a BookingResponseDTO into a BookingEvent for Kafka.
     * The sagaId and trainId are required so saga handlers can correlate events
     * back to the correct Booking and Train records.
     */
    private BookingEvent toBookingEvent(BookingResponseDTO dto, String eventType, Long trainId) {
        return BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sagaId(dto.getBookingNumber())             // correlation ID = bookingNumber
                .eventType(eventType)
                .occurredAt(LocalDateTime.now())
                .bookingId(dto.getId())
                .bookingNumber(dto.getBookingNumber())
                .routeId(dto.getRouteId())
                .trainId(trainId)                          // needed by SeatReservationSagaHandler
                .trainName(dto.getTrainName())
                .trainNumber(dto.getTrainNumber())
                .sourceStation(dto.getSourceStation())
                .destinationStation(dto.getDestinationStation())
                .departureTime(dto.getDepartureTime())
                .passengerName(dto.getPassengerName())
                .passengerEmail(dto.getPassengerEmail())
                .passengerPhone(dto.getPassengerPhone())
                .seatCount(dto.getSeatCount())
                .totalAmount(dto.getTotalAmount())
                .status(dto.getStatus())
                .build();
    }

    private BookingResponseDTO toResponseDTO(Booking booking) {
        Route route = booking.getRoute();
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .routeId(route.getId())
                .trainName(route.getTrain().getTrainName())
                .trainNumber(route.getTrain().getTrainNumber())
                .sourceStation(route.getSourceStation().getStationName()
                        + " (" + route.getSourceStation().getStationCode() + ")")
                .destinationStation(route.getDestinationStation().getStationName()
                        + " (" + route.getDestinationStation().getStationCode() + ")")
                .departureTime(route.getDepartureTime())
                .arrivalTime(route.getArrivalTime())
                .passengerName(booking.getPassengerName())
                .passengerEmail(booking.getPassengerEmail())
                .passengerPhone(booking.getPassengerPhone())
                .seatCount(booking.getSeatCount())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .failureReason(booking.getFailureReason())  // null unless saga compensation fired
                .build();
    }
}
