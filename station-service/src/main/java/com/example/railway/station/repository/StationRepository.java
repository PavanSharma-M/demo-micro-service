package com.example.railway.station.repository;

import com.example.railway.station.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByStationCode(String stationCode);
    boolean existsByStationCode(String stationCode);
}
