package com.example.railway.station.service;

import com.example.railway.station.dto.StationDTO;
import com.example.railway.station.exception.DuplicateResourceException;
import com.example.railway.station.exception.ResourceNotFoundException;
import com.example.railway.station.model.Station;
import com.example.railway.station.repository.StationRepository;
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
        return toDTO(stationRepository.findByStationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with code: " + code)));
    }

    public StationDTO createStation(StationDTO dto) {
        if (stationRepository.existsByStationCode(dto.getStationCode())) {
            throw new DuplicateResourceException("Station already exists with code: " + dto.getStationCode());
        }
        Station station = Station.builder()
                .stationCode(dto.getStationCode())
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
        station.setStationCode(dto.getStationCode());
        station.setStationName(dto.getStationName());
        station.setCity(dto.getCity());
        station.setState(dto.getState());
        return toDTO(stationRepository.save(station));
    }

    public void deleteStation(Long id) {
        findById(id);
        stationRepository.deleteById(id);
    }

    private Station findById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + id));
    }

    private StationDTO toDTO(Station station) {
        return StationDTO.builder()
                .id(station.getId())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .city(station.getCity())
                .state(station.getState())
                .build();
    }
}
