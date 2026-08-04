package com.example.demo_micro_service.service;

import com.example.demo_micro_service.dto.TrainDTO;
import com.example.demo_micro_service.exception.DuplicateResourceException;
import com.example.demo_micro_service.exception.ResourceNotFoundException;
import com.example.demo_micro_service.model.Train;
import com.example.demo_micro_service.repository.TrainRepository;
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
