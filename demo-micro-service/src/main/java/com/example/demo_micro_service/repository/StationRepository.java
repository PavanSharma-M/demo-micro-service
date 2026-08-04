package com.example.demo_micro_service.repository;

import com.example.demo_micro_service.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByStationCode(String stationCode);

    boolean existsByStationCode(String stationCode);
}
