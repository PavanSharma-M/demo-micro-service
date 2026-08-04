package com.example.demo_micro_service.service;

import com.example.demo_micro_service.dto.RouteDTO;
import com.example.demo_micro_service.exception.ResourceNotFoundException;
import com.example.demo_micro_service.model.Route;
import com.example.demo_micro_service.model.Station;
import com.example.demo_micro_service.model.Train;
import com.example.demo_micro_service.repository.RouteRepository;
import com.example.demo_micro_service.repository.StationRepository;
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
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private RouteService routeService;

    private Train buildTrain(Long id, String number, String name) {
        return Train.builder()
                .id(id)
                .trainNumber(number)
                .trainName(name)
                .trainType(Train.TrainType.EXPRESS)
                .totalSeats(100)
                .availableSeats(80)
                .build();
    }

    private Station buildStation(Long id, String code, String name, String city, String state) {
        return Station.builder()
                .id(id).stationCode(code).stationName(name).city(city).state(state).build();
    }

    private Route buildRoute(Long id, Train train, Station src, Station dst) {
        return Route.builder()
                .id(id)
                .train(train)
                .sourceStation(src)
                .destinationStation(dst)
                .departureTime(LocalDateTime.of(2026, 8, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 8, 1, 18, 0))
                .distanceKm(500)
                .fare(BigDecimal.valueOf(450))
                .build();
    }

    // ---- getAllRoutes ----

    @Test
    void getAllRoutes_returnsMappedDTOList() {
        Train train = buildTrain(1L, "12345", "Express");
        Station src = buildStation(1L, "MMCT", "Mumbai", "Mumbai", "MH");
        Station dst = buildStation(2L, "NDLS", "Delhi", "Delhi", "DL");
        given(routeRepository.findAll()).willReturn(List.of(buildRoute(1L, train, src, dst)));

        List<RouteDTO> result = routeService.getAllRoutes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTrainNumber()).isEqualTo("12345");
    }

    @Test
    void getAllRoutes_empty_returnsEmptyList() {
        given(routeRepository.findAll()).willReturn(List.of());

        assertThat(routeService.getAllRoutes()).isEmpty();
    }

    // ---- getRouteById ----

    @Test
    void getRouteById_existingId_returnsDTO() {
        Train train = buildTrain(1L, "12345", "Express");
        Station src = buildStation(1L, "MMCT", "Mumbai", "Mumbai", "MH");
        Station dst = buildStation(2L, "NDLS", "Delhi", "Delhi", "DL");
        given(routeRepository.findById(1L)).willReturn(Optional.of(buildRoute(1L, train, src, dst)));

        RouteDTO dto = routeService.getRouteById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getSourceStationCode()).isEqualTo("MMCT");
        assertThat(dto.getDestinationStationCode()).isEqualTo("NDLS");
    }

    @Test
    void getRouteById_nonExistingId_throwsResourceNotFoundException() {
        given(routeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.getRouteById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---- getRoutesByTrain ----

    @Test
    void getRoutesByTrain_returnsMatchingRoutes() {
        Train train = buildTrain(1L, "12345", "Express");
        Station src = buildStation(1L, "MMCT", "Mumbai", "Mumbai", "MH");
        Station dst = buildStation(2L, "NDLS", "Delhi", "Delhi", "DL");
        given(routeRepository.findByTrainId(1L)).willReturn(List.of(buildRoute(1L, train, src, dst)));

        List<RouteDTO> result = routeService.getRoutesByTrain(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrainId()).isEqualTo(1L);
    }

    // ---- searchRoutes ----

    @Test
    void searchRoutes_returnsMatchingRoutes() {
        Train train = buildTrain(1L, "12345", "Express");
        Station src = buildStation(1L, "MMCT", "Mumbai", "Mumbai", "MH");
        Station dst = buildStation(2L, "NDLS", "Delhi", "Delhi", "DL");
        given(routeRepository.findBySourceCodeAndDestinationCode("MMCT", "NDLS"))
                .willReturn(List.of(buildRoute(1L, train, src, dst)));

        List<RouteDTO> result = routeService.searchRoutes("MMCT", "NDLS");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSourceStationCode()).isEqualTo("MMCT");
    }

    @Test
    void searchRoutes_noMatch_returnsEmptyList() {
        given(routeRepository.findBySourceCodeAndDestinationCode("X", "Y")).willReturn(List.of());

        assertThat(routeService.searchRoutes("X", "Y")).isEmpty();
    }

    // ---- createRoute ----

    @Test
    void createRoute_validData_savesAndReturnsDTO() {
        Train train = buildTrain(1L, "12345", "Express");
        Station src = buildStation(1L, "MMCT", "Mumbai", "Mumbai", "MH");
        Station dst = buildStation(2L, "NDLS", "Delhi", "Delhi", "DL");

        RouteDTO dto = RouteDTO.builder()
                .trainId(1L)
                .sourceStationId(1L)
                .destinationStationId(2L)
                .departureTime(LocalDateTime.of(2026, 8, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 8, 1, 18, 0))
                .distanceKm(500)
                .fare(BigDecimal.valueOf(450))
                .build();

        given(trainRepository.findById(1L)).willReturn(Optional.of(train));
        given(stationRepository.findById(1L)).willReturn(Optional.of(src));
        given(stationRepository.findById(2L)).willReturn(Optional.of(dst));
        given(routeRepository.save(any(Route.class))).willAnswer(inv -> {
            Route r = inv.getArgument(0);
            return Route.builder()
                    .id(10L)
                    .train(r.getTrain())
                    .sourceStation(r.getSourceStation())
                    .destinationStation(r.getDestinationStation())
                    .departureTime(r.getDepartureTime())
                    .arrivalTime(r.getArrivalTime())
                    .distanceKm(r.getDistanceKm())
                    .fare(r.getFare())
                    .build();
        });

        RouteDTO result = routeService.createRoute(dto);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTrainNumber()).isEqualTo("12345");
        assertThat(result.getSourceStationCode()).isEqualTo("MMCT");
        assertThat(result.getDestinationStationCode()).isEqualTo("NDLS");
        assertThat(result.getFare()).isEqualByComparingTo(BigDecimal.valueOf(450));
    }

    @Test
    void createRoute_trainNotFound_throwsResourceNotFoundException() {
        RouteDTO dto = RouteDTO.builder()
                .trainId(99L).sourceStationId(1L).destinationStationId(2L)
                .departureTime(LocalDateTime.now()).arrivalTime(LocalDateTime.now().plusHours(5))
                .distanceKm(100).fare(BigDecimal.TEN).build();
        given(trainRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.createRoute(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Train");
    }

    @Test
    void createRoute_sourceStationNotFound_throwsResourceNotFoundException() {
        RouteDTO dto = RouteDTO.builder()
                .trainId(1L).sourceStationId(99L).destinationStationId(2L)
                .departureTime(LocalDateTime.now()).arrivalTime(LocalDateTime.now().plusHours(5))
                .distanceKm(100).fare(BigDecimal.TEN).build();
        given(trainRepository.findById(1L)).willReturn(Optional.of(buildTrain(1L, "X", "X")));
        given(stationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.createRoute(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("station");
    }

    // ---- deleteRoute ----

    @Test
    void deleteRoute_existingId_callsDeleteById() {
        Train train = buildTrain(1L, "12345", "Express");
        Station src = buildStation(1L, "MMCT", "Mumbai", "Mumbai", "MH");
        Station dst = buildStation(2L, "NDLS", "Delhi", "Delhi", "DL");
        given(routeRepository.findById(1L)).willReturn(Optional.of(buildRoute(1L, train, src, dst)));
        willDoNothing().given(routeRepository).deleteById(1L);

        routeService.deleteRoute(1L);

        then(routeRepository).should().deleteById(1L);
    }

    @Test
    void deleteRoute_nonExistingId_throwsResourceNotFoundException() {
        given(routeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.deleteRoute(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- toDTO ----

    @Test
    void toDTO_mapsAllFieldsCorrectly() {
        Train train = buildTrain(1L, "12345", "Express");
        Station src = buildStation(1L, "MMCT", "Mumbai Central", "Mumbai", "MH");
        Station dst = buildStation(2L, "NDLS", "New Delhi", "Delhi", "DL");
        Route route = buildRoute(5L, train, src, dst);

        RouteDTO dto = routeService.toDTO(route);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getTrainId()).isEqualTo(1L);
        assertThat(dto.getTrainNumber()).isEqualTo("12345");
        assertThat(dto.getTrainName()).isEqualTo("Express");
        assertThat(dto.getSourceStationId()).isEqualTo(1L);
        assertThat(dto.getSourceStationCode()).isEqualTo("MMCT");
        assertThat(dto.getSourceStationName()).isEqualTo("Mumbai Central");
        assertThat(dto.getDestinationStationId()).isEqualTo(2L);
        assertThat(dto.getDestinationStationCode()).isEqualTo("NDLS");
        assertThat(dto.getDestinationStationName()).isEqualTo("New Delhi");
        assertThat(dto.getDistanceKm()).isEqualTo(500);
        assertThat(dto.getFare()).isEqualByComparingTo(BigDecimal.valueOf(450));
    }
}
