package com.example.railway.route.repository;

import com.example.railway.route.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByTrainId(Long trainId);
    List<Route> findBySourceStationIdAndDestinationStationId(Long sourceStationId, Long destinationStationId);
}
