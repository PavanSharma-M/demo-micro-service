package com.example.demo_micro_service.service;

import com.example.demo_micro_service.dto.StationDTO;
import com.example.demo_micro_service.exception.DuplicateResourceException;
import com.example.demo_micro_service.exception.ResourceNotFoundException;
import com.example.demo_micro_service.model.Station;
import com.example.demo_micro_service.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StationService {

    private final StationRepository stationRepository;

    public List<StationDTO> getAllStations() {
        return stationRepository.findAll().stream().map(this::toDTO).toList();
    }

    public StationDTO getStationById(Long id) {
        return toDTO(findById(id));
    }

    public StationDTO getStationByCode(String code) {
        Station station = stationRepository.findByStationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with code: " + code));
        return toDTO(station);
    }

    public StationDTO createStation(StationDTO dto) {
        if (stationRepository.existsByStationCode(dto.getStationCode())) {
            throw new DuplicateResourceException("Station already exists with code: " + dto.getStationCode());
        }
        Station station = Station.builder()
                .stationCode(dto.getStationCode().toUpperCase())
                .stationName(dto.getStationName())
                .city(dto.getCity())
                .state(dto.getState())
                .build();
        return toDTO(stationRepository.save(station));
    }

    public StationDTO updateStation(Long id, StationDTO dto) {
        Station station = findById(id);
        if (!station.getStationCode().equals(dto.getStationCode())
                && stationRepository.existsByStationCode(dto.getStationCode())) {
            throw new DuplicateResourceException("Station already exists with code: " + dto.getStationCode());
        }
        station.setStationCode(dto.getStationCode().toUpperCase());
        station.setStationName(dto.getStationName());
        station.setCity(dto.getCity());
        station.setState(dto.getState());
        return toDTO(stationRepository.save(station));
    }

    public void deleteStation(Long id) {
        findById(id);
        stationRepository.deleteById(id);
    }

    public Station findById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + id));
    }

    public StationDTO toDTO(Station station) {
        return StationDTO.builder()
                .id(station.getId())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .city(station.getCity())
                .state(station.getState())
                .build();
    }
}
