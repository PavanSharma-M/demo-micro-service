package com.example.railway.train.service;

import com.example.railway.train.dto.TrainDTO;
import com.example.railway.train.exception.DuplicateResourceException;
import com.example.railway.train.exception.ResourceNotFoundException;
import com.example.railway.train.model.Train;
import com.example.railway.train.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TrainService {

    private final TrainRepository trainRepository;

    public List<TrainDTO> getAllTrains() {
        return trainRepository.findAll().stream().map(this::toDTO).toList();
    }

    public TrainDTO getTrainById(Long id) {
        return toDTO(findById(id));
    }

    public TrainDTO getTrainByNumber(String trainNumber) {
        Train train = trainRepository.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with number: " + trainNumber));
        return toDTO(train);
    }

    public TrainDTO createTrain(TrainDTO dto) {
        log.info("Creating train: number={} name={}", dto.getTrainNumber(), dto.getTrainName());
        if (trainRepository.existsByTrainNumber(dto.getTrainNumber())) {
            log.warn("Duplicate train number: {}", dto.getTrainNumber());
            throw new DuplicateResourceException("Train already exists with number: " + dto.getTrainNumber());
        }
        Train train = Train.builder()
                .trainNumber(dto.getTrainNumber())
                .trainName(dto.getTrainName())
                .trainType(dto.getTrainType())
                .totalSeats(dto.getTotalSeats())
                .availableSeats(dto.getTotalSeats())
                .build();
        TrainDTO result = toDTO(trainRepository.save(train));
        log.info("Train created: id={} number={}", result.getId(), result.getTrainNumber());
        return result;
    }

    public TrainDTO updateTrain(Long id, TrainDTO dto) {
        log.info("Updating train id={}", id);
        Train train = findById(id);
        if (!train.getTrainNumber().equals(dto.getTrainNumber())
                && trainRepository.existsByTrainNumber(dto.getTrainNumber())) {
            throw new DuplicateResourceException("Train already exists with number: " + dto.getTrainNumber());
        }
        train.setTrainNumber(dto.getTrainNumber());
        train.setTrainName(dto.getTrainName());
        train.setTrainType(dto.getTrainType());
        train.setTotalSeats(dto.getTotalSeats());
        return toDTO(trainRepository.save(train));
    }

    public void deleteTrain(Long id) {
        log.info("Deleting train id={}", id);
        findById(id);
        trainRepository.deleteById(id);
        log.info("Train id={} deleted", id);
    }

    /**
     * Updates available seats by applying a delta.
     * Positive delta = releasing seats (cancellation).
     * Negative delta = reserving seats (booking).
     */
    public TrainDTO updateAvailableSeats(Long id, int delta) {
        log.info("Updating seats for trainId={} delta={}", id, delta);
        Train train = findById(id);
        int updated = train.getAvailableSeats() + delta;
        if (updated < 0) {
            log.warn("Seat update rejected: trainId={} available={} requested={}",
                    id, train.getAvailableSeats(), Math.abs(delta));
            throw new IllegalArgumentException(
                    "Not enough seats. Available: " + train.getAvailableSeats() + ", requested: " + Math.abs(delta));
        }
        if (updated > train.getTotalSeats()) {
            updated = train.getTotalSeats();
        }
        train.setAvailableSeats(updated);
        TrainDTO result = toDTO(trainRepository.save(train));
        log.info("Seats updated for trainId={}: availableSeats={}", id, result.getAvailableSeats());
        return result;
    }

    private Train findById(Long id) {
        return trainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + id));
    }

    public TrainDTO toDTO(Train train) {
        return TrainDTO.builder()
                .id(train.getId())
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .trainType(train.getTrainType())
                .totalSeats(train.getTotalSeats())
                .availableSeats(train.getAvailableSeats())
                .build();
    }
}
