package com.example.demo_micro_service.repository;

import com.example.demo_micro_service.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByTrainId(Long trainId);

    @Query("SELECT r FROM Route r WHERE r.sourceStation.id = :sourceId AND r.destinationStation.id = :destinationId")
    List<Route> findBySourceAndDestination(@Param("sourceId") Long sourceId,
                                           @Param("destinationId") Long destinationId);

    @Query("SELECT r FROM Route r WHERE r.sourceStation.stationCode = :sourceCode AND r.destinationStation.stationCode = :destCode")
    List<Route> findBySourceCodeAndDestinationCode(@Param("sourceCode") String sourceCode,
                                                   @Param("destCode") String destCode);
}
