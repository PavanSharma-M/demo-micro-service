package com.example.demo_micro_service.service;

import com.example.demo_micro_service.dto.RouteDTO;
import com.example.demo_micro_service.exception.ResourceNotFoundException;
import com.example.demo_micro_service.model.Route;
import com.example.demo_micro_service.model.Station;
import com.example.demo_micro_service.model.Train;
import com.example.demo_micro_service.repository.RouteRepository;
import com.example.demo_micro_service.repository.StationRepository;
import com.example.demo_micro_service.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteService {

    private final RouteRepository routeRepository;
    private final TrainRepository trainRepository;
    private final StationRepository stationRepository;

    public List<RouteDTO> getAllRoutes() {
        return routeRepository.findAll().stream().map(this::toDTO).toList();
    }

    public RouteDTO getRouteById(Long id) {
        return toDTO(findById(id));
    }

    public List<RouteDTO> getRoutesByTrain(Long trainId) {
        return routeRepository.findByTrainId(trainId).stream().map(this::toDTO).toList();
    }

    public List<RouteDTO> searchRoutes(String sourceCode, String destinationCode) {
        return routeRepository.findBySourceCodeAndDestinationCode(sourceCode, destinationCode)
                .stream().map(this::toDTO).toList();
    }

    public RouteDTO createRoute(RouteDTO dto) {
        Train train = trainRepository.findById(dto.getTrainId())
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + dto.getTrainId()));
        Station source = stationRepository.findById(dto.getSourceStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Source station not found with id: " + dto.getSourceStationId()));
        Station destination = stationRepository.findById(dto.getDestinationStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination station not found with id: " + dto.getDestinationStationId()));

        Route route = Route.builder()
                .train(train)
                .sourceStation(source)
                .destinationStation(destination)
                .departureTime(dto.getDepartureTime())
                .arrivalTime(dto.getArrivalTime())
                .distanceKm(dto.getDistanceKm())
                .fare(dto.getFare())
                .build();
        return toDTO(routeRepository.save(route));
    }

    public RouteDTO updateRoute(Long id, RouteDTO dto) {
        Route route = findById(id);
        Train train = trainRepository.findById(dto.getTrainId())
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + dto.getTrainId()));
        Station source = stationRepository.findById(dto.getSourceStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Source station not found with id: " + dto.getSourceStationId()));
        Station destination = stationRepository.findById(dto.getDestinationStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination station not found with id: " + dto.getDestinationStationId()));

        route.setTrain(train);
        route.setSourceStation(source);
        route.setDestinationStation(destination);
        route.setDepartureTime(dto.getDepartureTime());
        route.setArrivalTime(dto.getArrivalTime());
        route.setDistanceKm(dto.getDistanceKm());
        route.setFare(dto.getFare());
        return toDTO(routeRepository.save(route));
    }

    public void deleteRoute(Long id) {
        findById(id);
        routeRepository.deleteById(id);
    }

    public Route findById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));
    }

    public RouteDTO toDTO(Route route) {
        return RouteDTO.builder()
                .id(route.getId())
                .trainId(route.getTrain().getId())
                .trainNumber(route.getTrain().getTrainNumber())
                .trainName(route.getTrain().getTrainName())
                .sourceStationId(route.getSourceStation().getId())
                .sourceStationCode(route.getSourceStation().getStationCode())
                .sourceStationName(route.getSourceStation().getStationName())
                .destinationStationId(route.getDestinationStation().getId())
                .destinationStationCode(route.getDestinationStation().getStationCode())
                .destinationStationName(route.getDestinationStation().getStationName())
                .departureTime(route.getDepartureTime())
                .arrivalTime(route.getArrivalTime())
                .distanceKm(route.getDistanceKm())
                .fare(route.getFare())
                .build();
    }
}
