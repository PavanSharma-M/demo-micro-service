package com.example.demo_micro_service.service;

import com.example.demo_micro_service.dto.BookingRequestDTO;
import com.example.demo_micro_service.dto.BookingResponseDTO;
import com.example.demo_micro_service.exception.BookingException;
import com.example.demo_micro_service.exception.ResourceNotFoundException;
import com.example.demo_micro_service.kafka.producer.BookingEventProducer;
import com.example.demo_micro_service.model.Booking;
import com.example.demo_micro_service.model.Route;
import com.example.demo_micro_service.model.Station;
import com.example.demo_micro_service.model.Train;
import com.example.demo_micro_service.repository.BookingRepository;
import com.example.demo_micro_service.repository.TrainRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RouteService routeService;

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private BookingEventProducer bookingEventProducer;

    @InjectMocks
    private BookingService bookingService;

    /** Builds a fully-populated Route with nested Train, source and destination Station. */
    private Route buildRoute(int availableSeats, BigDecimal fare) {
        Train train = Train.builder()
                .id(1L)
                .trainNumber("12345")
                .trainName("Rajdhani")
                .trainType(Train.TrainType.RAJDHANI)
                .totalSeats(100)
                .availableSeats(availableSeats)
                .build();
        Station src = Station.builder()
                .id(1L).stationCode("MMCT").stationName("Mumbai Central").city("Mumbai").state("MH").build();
        Station dst = Station.builder()
                .id(2L).stationCode("NDLS").stationName("New Delhi").city("Delhi").state("DL").build();
        return Route.builder()
                .id(1L)
                .train(train)
                .sourceStation(src)
                .destinationStation(dst)
                .departureTime(LocalDateTime.of(2026, 8, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 8, 1, 18, 0))
                .distanceKm(500)
                .fare(fare)
                .build();
    }

    /** Builds a Booking with the given Route and status, ready to be returned from repository mocks. */
    private Booking buildBooking(Long id, Route route, Booking.BookingStatus status, int seatCount) {
        return Booking.builder()
                .id(id)
                .bookingNumber("RLY-20260801-ABCD1234")
                .route(route)
                .passengerName("John Doe")
                .passengerEmail("john@example.com")
                .passengerPhone("9876543210")
                .seatCount(seatCount)
                .bookingDate(LocalDateTime.now())
                .status(status)
                .totalAmount(route.getFare().multiply(BigDecimal.valueOf(seatCount)))
                .build();
    }

    // ---- createBooking ----

    @Test
    void createBooking_sufficientSeats_createsPendingBooking() {
        Route route = buildRoute(50, BigDecimal.valueOf(500));
        BookingRequestDTO request = BookingRequestDTO.builder()
                .routeId(1L)
                .passengerName("John Doe")
                .passengerEmail("john@example.com")
                .passengerPhone("9876543210")
                .seatCount(2)
                .build();

        given(routeService.findById(1L)).willReturn(route);
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> {
            Booking b = inv.getArgument(0);
            return Booking.builder()
                    .id(10L)
                    .bookingNumber(b.getBookingNumber())
                    .route(route)
                    .passengerName(b.getPassengerName())
                    .passengerEmail(b.getPassengerEmail())
                    .passengerPhone(b.getPassengerPhone())
                    .seatCount(b.getSeatCount())
                    .bookingDate(b.getBookingDate())
                    .status(b.getStatus())
                    .totalAmount(b.getTotalAmount())
                    .build();
        });

        BookingResponseDTO result = bookingService.createBooking(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getPassengerName()).isEqualTo("John Doe");
        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
        assertThat(result.getSeatCount()).isEqualTo(2);
        // totalAmount = 500 fare × 2 seats
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        // Seat reservation is performed asynchronously by the saga.
        assertThat(route.getTrain().getAvailableSeats()).isEqualTo(50);
        then(bookingEventProducer).should().publishBookingInitiated(any());
    }

    @Test
    void createBooking_insufficientSeats_startsSagaWithoutReservingSeats() {
        Route route = buildRoute(1, BigDecimal.valueOf(500));
        BookingRequestDTO request = BookingRequestDTO.builder()
                .routeId(1L)
                .passengerName("Jane Doe")
                .passengerEmail("jane@example.com")
                .passengerPhone("9876543210")
                .seatCount(5)
                .build();

        given(routeService.findById(1L)).willReturn(route);
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.createBooking(request);

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
        assertThat(route.getTrain().getAvailableSeats()).isEqualTo(1);
        then(bookingEventProducer).should().publishBookingInitiated(any());
    }

    @Test
    void createBooking_exactlyEnoughSeats_createsBookingSuccessfully() {
        Route route = buildRoute(3, BigDecimal.valueOf(200));
        BookingRequestDTO request = BookingRequestDTO.builder()
                .routeId(1L)
                .passengerName("Alice")
                .passengerEmail("alice@example.com")
                .passengerPhone("9876543210")
                .seatCount(3)
                .build();

        given(routeService.findById(1L)).willReturn(route);
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> {
            Booking b = inv.getArgument(0);
            return Booking.builder()
                    .id(20L).bookingNumber(b.getBookingNumber()).route(route)
                    .passengerName(b.getPassengerName()).passengerEmail(b.getPassengerEmail())
                    .passengerPhone(b.getPassengerPhone()).seatCount(b.getSeatCount())
                    .bookingDate(b.getBookingDate()).status(b.getStatus()).totalAmount(b.getTotalAmount())
                    .build();
        });

        BookingResponseDTO result = bookingService.createBooking(request);

        assertThat(result.getSeatCount()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
        assertThat(route.getTrain().getAvailableSeats()).isEqualTo(3);
    }

    // ---- getBookingById ----

    @Test
    void getBookingById_existingId_returnsDTO() {
        Route route = buildRoute(50, BigDecimal.valueOf(500));
        Booking booking = buildBooking(5L, route, Booking.BookingStatus.CONFIRMED, 2);
        given(bookingRepository.findById(5L)).willReturn(Optional.of(booking));

        BookingResponseDTO result = bookingService.getBookingById(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getPassengerName()).isEqualTo("John Doe");
        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
    }

    @Test
    void getBookingById_nonExistingId_throwsResourceNotFoundException() {
        given(bookingRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---- getBookingByNumber ----

    @Test
    void getBookingByNumber_existingNumber_returnsDTO() {
        Route route = buildRoute(50, BigDecimal.valueOf(300));
        Booking booking = buildBooking(3L, route, Booking.BookingStatus.CONFIRMED, 1);
        given(bookingRepository.findByBookingNumber("RLY-20260801-ABCD1234"))
                .willReturn(Optional.of(booking));

        BookingResponseDTO result = bookingService.getBookingByNumber("RLY-20260801-ABCD1234");

        assertThat(result.getBookingNumber()).isEqualTo("RLY-20260801-ABCD1234");
    }

    @Test
    void getBookingByNumber_nonExistingNumber_throwsResourceNotFoundException() {
        given(bookingRepository.findByBookingNumber("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingByNumber("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ---- getBookingsByEmail ----

    @Test
    void getBookingsByEmail_returnsMatchingBookings() {
        Route route = buildRoute(50, BigDecimal.valueOf(400));
        Booking booking = buildBooking(1L, route, Booking.BookingStatus.CONFIRMED, 1);
        given(bookingRepository.findByPassengerEmail("john@example.com"))
                .willReturn(List.of(booking));

        List<BookingResponseDTO> result = bookingService.getBookingsByEmail("john@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPassengerEmail()).isEqualTo("john@example.com");
    }

    @Test
    void getBookingsByEmail_noMatches_returnsEmptyList() {
        given(bookingRepository.findByPassengerEmail("none@example.com")).willReturn(List.of());

        assertThat(bookingService.getBookingsByEmail("none@example.com")).isEmpty();
    }

    // ---- getAllBookings ----

    @Test
    void getAllBookings_returnsMappedList() {
        Route route = buildRoute(50, BigDecimal.valueOf(400));
        given(bookingRepository.findAll()).willReturn(
                List.of(buildBooking(1L, route, Booking.BookingStatus.CONFIRMED, 2)));

        List<BookingResponseDTO> result = bookingService.getAllBookings();

        assertThat(result).hasSize(1);
    }

    // ---- cancelBooking ----

    @Test
    void cancelBooking_confirmedBooking_cancelledAndSeatsRestored() {
        Route route = buildRoute(48, BigDecimal.valueOf(500));
        Booking booking = buildBooking(7L, route, Booking.BookingStatus.CONFIRMED, 2);

        given(bookingRepository.findById(7L)).willReturn(Optional.of(booking));
        given(trainRepository.save(any(Train.class))).willAnswer(inv -> inv.getArgument(0));
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.cancelBooking(7L);

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.CANCELLED);
        // availableSeats should be restored: 48 + 2 = 50
        assertThat(route.getTrain().getAvailableSeats()).isEqualTo(50);
        then(trainRepository).should().save(route.getTrain());
    }

    @Test
    void cancelBooking_alreadyCancelledBooking_throwsBookingException() {
        Route route = buildRoute(50, BigDecimal.valueOf(500));
        Booking booking = buildBooking(8L, route, Booking.BookingStatus.CANCELLED, 2);

        given(bookingRepository.findById(8L)).willReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(8L))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancelBooking_nonExistingId_throwsResourceNotFoundException() {
        given(bookingRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
