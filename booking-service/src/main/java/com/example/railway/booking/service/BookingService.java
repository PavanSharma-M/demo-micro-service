package com.example.railway.booking.service;

import com.example.railway.booking.client.RouteClient;
import com.example.railway.booking.client.TrainClient;
import com.example.railway.booking.dto.BookingRequestDTO;
import com.example.railway.booking.dto.BookingResponseDTO;
import com.example.railway.booking.dto.RouteDTO;
import com.example.railway.booking.exception.BookingException;
import com.example.railway.booking.exception.ResourceNotFoundException;
import com.example.railway.booking.model.Booking;
import com.example.railway.booking.repository.BookingRepository;
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
    private final RouteClient routeClient;
    private final TrainClient trainClient;

    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        // Fetch route details from route-service (includes trainId + fare)
        RouteDTO route = routeClient.getRouteById(request.getRouteId());

        if (route.getAvailableSeats() < request.getSeatCount()) {
            throw new BookingException("Not enough seats available. Available: " + route.getAvailableSeats());
        }

        // Reserve seats in train-service
        trainClient.updateSeats(route.getTrainId(), -request.getSeatCount());

        Booking booking = Booking.builder()
                .bookingNumber(generateBookingNumber())
                .routeId(request.getRouteId())
                .passengerName(request.getPassengerName())
                .passengerEmail(request.getPassengerEmail())
                .passengerPhone(request.getPassengerPhone())
                .seatCount(request.getSeatCount())
                .bookingDate(LocalDateTime.now())
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(route.getFare().multiply(java.math.BigDecimal.valueOf(request.getSeatCount())))
                .build();

        return toResponseDTO(bookingRepository.save(booking), route);
    }

    public BookingResponseDTO getBookingById(Long id) {
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
        Booking booking = findById(id);
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }

        // Fetch routeId → trainId to release the seats
        RouteDTO route = routeClient.getRouteById(booking.getRouteId());
        trainClient.updateSeats(route.getTrainId(), booking.getSeatCount()); // positive delta = release

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return toResponseDTO(bookingRepository.save(booking), route);
    }

    private Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private String generateBookingNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "BK" + timestamp + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
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
                .totalAmount(booking.getTotalAmount())
                .build();
    }
}
