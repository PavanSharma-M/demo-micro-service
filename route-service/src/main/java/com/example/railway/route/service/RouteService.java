package com.example.railway.route.service;

import com.example.railway.route.client.StationClient;
import com.example.railway.route.client.TrainClient;
import com.example.railway.route.dto.RouteDTO;
import com.example.railway.route.dto.StationDTO;
import com.example.railway.route.dto.TrainDTO;
import com.example.railway.route.exception.ResourceNotFoundException;
import com.example.railway.route.model.Route;
import com.example.railway.route.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteService {

    private final RouteRepository routeRepository;
    private final TrainClient trainClient;
    private final StationClient stationClient;

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
        StationDTO source = stationClient.getStationByCode(sourceCode);
        StationDTO destination = stationClient.getStationByCode(destinationCode);
        return routeRepository
                .findBySourceStationIdAndDestinationStationId(source.getId(), destination.getId())
                .stream().map(this::toDTO).toList();
    }

    public RouteDTO createRoute(RouteDTO dto) {
        // Validate that train and stations exist in their respective services
        trainClient.getTrainById(dto.getTrainId());
        stationClient.getStationById(dto.getSourceStationId());
        stationClient.getStationById(dto.getDestinationStationId());

        Route route = Route.builder()
                .trainId(dto.getTrainId())
                .sourceStationId(dto.getSourceStationId())
                .destinationStationId(dto.getDestinationStationId())
                .departureTime(dto.getDepartureTime())
                .arrivalTime(dto.getArrivalTime())
                .distanceKm(dto.getDistanceKm())
                .fare(dto.getFare())
                .build();
        return toDTO(routeRepository.save(route));
    }

    public RouteDTO updateRoute(Long id, RouteDTO dto) {
        Route route = findById(id);
        trainClient.getTrainById(dto.getTrainId());
        stationClient.getStationById(dto.getSourceStationId());
        stationClient.getStationById(dto.getDestinationStationId());

        route.setTrainId(dto.getTrainId());
        route.setSourceStationId(dto.getSourceStationId());
        route.setDestinationStationId(dto.getDestinationStationId());
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

    private RouteDTO toDTO(Route route) {
        TrainDTO train = trainClient.getTrainById(route.getTrainId());
        StationDTO source = stationClient.getStationById(route.getSourceStationId());
        StationDTO destination = stationClient.getStationById(route.getDestinationStationId());

        return RouteDTO.builder()
                .id(route.getId())
                .trainId(route.getTrainId())
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .trainType(train.getTrainType())
                .availableSeats(train.getAvailableSeats())
                .sourceStationId(route.getSourceStationId())
                .sourceStationCode(source.getStationCode())
                .sourceStationName(source.getStationName())
                .sourceCity(source.getCity())
                .destinationStationId(route.getDestinationStationId())
                .destinationStationCode(destination.getStationCode())
                .destinationStationName(destination.getStationName())
                .destinationCity(destination.getCity())
                .departureTime(route.getDepartureTime())
                .arrivalTime(route.getArrivalTime())
                .distanceKm(route.getDistanceKm())
                .fare(route.getFare())
                .build();
    }
}
