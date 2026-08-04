package com.example.railway.train.service;

import com.example.railway.train.dto.TrainDTO;
import com.example.railway.train.exception.DuplicateResourceException;
import com.example.railway.train.exception.ResourceNotFoundException;
import com.example.railway.train.model.Train;
import com.example.railway.train.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        if (trainRepository.existsByTrainNumber(dto.getTrainNumber())) {
            throw new DuplicateResourceException("Train already exists with number: " + dto.getTrainNumber());
        }
        Train train = Train.builder()
                .trainNumber(dto.getTrainNumber())
                .trainName(dto.getTrainName())
                .trainType(dto.getTrainType())
                .totalSeats(dto.getTotalSeats())
                .availableSeats(dto.getTotalSeats())
                .build();
        return toDTO(trainRepository.save(train));
    }

    public TrainDTO updateTrain(Long id, TrainDTO dto) {
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
        findById(id);
        trainRepository.deleteById(id);
    }

    /**
     * Updates available seats by applying a delta.
     * Positive delta = releasing seats (cancellation).
     * Negative delta = reserving seats (booking).
     */
    public TrainDTO updateAvailableSeats(Long id, int delta) {
        Train train = findById(id);
        int updated = train.getAvailableSeats() + delta;
        if (updated < 0) {
            throw new IllegalArgumentException(
                    "Not enough seats. Available: " + train.getAvailableSeats() + ", requested: " + Math.abs(delta));
        }
        if (updated > train.getTotalSeats()) {
            updated = train.getTotalSeats();
        }
        train.setAvailableSeats(updated);
        return toDTO(trainRepository.save(train));
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
