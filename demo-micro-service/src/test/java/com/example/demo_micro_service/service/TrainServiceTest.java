package com.example.demo_micro_service.service;

import com.example.demo_micro_service.dto.TrainDTO;
import com.example.demo_micro_service.exception.DuplicateResourceException;
import com.example.demo_micro_service.exception.ResourceNotFoundException;
import com.example.demo_micro_service.model.Train;
import com.example.demo_micro_service.repository.TrainRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TrainServiceTest {

    @Mock
    private TrainRepository trainRepository;

    @InjectMocks
    private TrainService trainService;

    private Train buildTrain(Long id, String number, String name, int total, int available) {
        return Train.builder()
                .id(id)
                .trainNumber(number)
                .trainName(name)
                .trainType(Train.TrainType.EXPRESS)
                .totalSeats(total)
                .availableSeats(available)
                .build();
    }

    // ---- getAllTrains ----

    @Test
    void getAllTrains_returnsMappedDTOList() {
        given(trainRepository.findAll()).willReturn(
                List.of(buildTrain(1L, "12345", "Express One", 100, 80)));

        List<TrainDTO> result = trainService.getAllTrains();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrainNumber()).isEqualTo("12345");
        assertThat(result.get(0).getAvailableSeats()).isEqualTo(80);
    }

    @Test
    void getAllTrains_empty_returnsEmptyList() {
        given(trainRepository.findAll()).willReturn(List.of());

        assertThat(trainService.getAllTrains()).isEmpty();
    }

    // ---- getTrainById ----

    @Test
    void getTrainById_existingId_returnsTrain() {
        given(trainRepository.findById(1L))
                .willReturn(Optional.of(buildTrain(1L, "12345", "Express One", 100, 80)));

        TrainDTO dto = trainService.getTrainById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTrainNumber()).isEqualTo("12345");
    }

    @Test
    void getTrainById_nonExistingId_throwsResourceNotFoundException() {
        given(trainRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> trainService.getTrainById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---- getTrainByNumber ----

    @Test
    void getTrainByNumber_existingNumber_returnsDTO() {
        given(trainRepository.findByTrainNumber("12345"))
                .willReturn(Optional.of(buildTrain(1L, "12345", "Express", 100, 80)));

        TrainDTO dto = trainService.getTrainByNumber("12345");

        assertThat(dto.getTrainNumber()).isEqualTo("12345");
    }

    @Test
    void getTrainByNumber_nonExistingNumber_throwsResourceNotFoundException() {
        given(trainRepository.findByTrainNumber("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> trainService.getTrainByNumber("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ---- createTrain ----

    @Test
    void createTrain_newTrainNumber_savesAndReturnsTrain() {
        TrainDTO dto = TrainDTO.builder()
                .trainNumber("12345")
                .trainName("Express One")
                .trainType(Train.TrainType.EXPRESS)
                .totalSeats(100)
                .build();
        given(trainRepository.existsByTrainNumber("12345")).willReturn(false);
        given(trainRepository.save(any(Train.class))).willAnswer(inv -> {
            Train t = inv.getArgument(0);
            return Train.builder()
                    .id(1L)
                    .trainNumber(t.getTrainNumber())
                    .trainName(t.getTrainName())
                    .trainType(t.getTrainType())
                    .totalSeats(t.getTotalSeats())
                    .availableSeats(t.getAvailableSeats())
                    .build();
        });

        TrainDTO result = trainService.createTrain(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTrainNumber()).isEqualTo("12345");
        // availableSeats should equal totalSeats on creation
        assertThat(result.getAvailableSeats()).isEqualTo(100);
    }

    @Test
    void createTrain_duplicateTrainNumber_throwsDuplicateResourceException() {
        TrainDTO dto = TrainDTO.builder()
                .trainNumber("EXIST")
                .trainName("X")
                .trainType(Train.TrainType.LOCAL)
                .totalSeats(50)
                .build();
        given(trainRepository.existsByTrainNumber("EXIST")).willReturn(true);

        assertThatThrownBy(() -> trainService.createTrain(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("EXIST");
    }

    // ---- updateTrain ----

    @Test
    void updateTrain_sameTrainNumber_updatesSuccessfully() {
        Train existing = buildTrain(1L, "12345", "Old Name", 100, 80);
        TrainDTO dto = TrainDTO.builder()
                .trainNumber("12345")
                .trainName("New Name")
                .trainType(Train.TrainType.RAJDHANI)
                .totalSeats(120)
                .build();

        given(trainRepository.findById(1L)).willReturn(Optional.of(existing));
        given(trainRepository.save(any(Train.class))).willAnswer(inv -> inv.getArgument(0));

        TrainDTO result = trainService.updateTrain(1L, dto);

        assertThat(result.getTrainName()).isEqualTo("New Name");
        assertThat(result.getTotalSeats()).isEqualTo(120);
        assertThat(result.getTrainType()).isEqualTo(Train.TrainType.RAJDHANI);
    }

    @Test
    void updateTrain_changedNumberToDuplicate_throwsDuplicateResourceException() {
        Train existing = buildTrain(1L, "12345", "Express", 100, 80);
        TrainDTO dto = TrainDTO.builder()
                .trainNumber("TAKEN")
                .trainName("X")
                .trainType(Train.TrainType.LOCAL)
                .totalSeats(50)
                .build();

        given(trainRepository.findById(1L)).willReturn(Optional.of(existing));
        given(trainRepository.existsByTrainNumber("TAKEN")).willReturn(true);

        assertThatThrownBy(() -> trainService.updateTrain(1L, dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("TAKEN");
    }

    @Test
    void updateTrain_nonExistingId_throwsResourceNotFoundException() {
        given(trainRepository.findById(99L)).willReturn(Optional.empty());
        TrainDTO dto = TrainDTO.builder().trainNumber("X").trainName("X").trainType(Train.TrainType.LOCAL).totalSeats(1).build();

        assertThatThrownBy(() -> trainService.updateTrain(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- deleteTrain ----

    @Test
    void deleteTrain_existingId_callsDeleteById() {
        given(trainRepository.findById(1L))
                .willReturn(Optional.of(buildTrain(1L, "12345", "Exp", 100, 80)));
        willDoNothing().given(trainRepository).deleteById(1L);

        trainService.deleteTrain(1L);

        then(trainRepository).should().deleteById(1L);
    }

    @Test
    void deleteTrain_nonExistingId_throwsResourceNotFoundException() {
        given(trainRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> trainService.deleteTrain(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- toDTO ----

    @Test
    void toDTO_mapsAllFieldsCorrectly() {
        Train train = Train.builder()
                .id(5L)
                .trainNumber("99999")
                .trainName("Shatabdi")
                .trainType(Train.TrainType.SHATABDI)
                .totalSeats(200)
                .availableSeats(150)
                .build();

        TrainDTO dto = trainService.toDTO(train);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getTrainNumber()).isEqualTo("99999");
        assertThat(dto.getTrainName()).isEqualTo("Shatabdi");
        assertThat(dto.getTrainType()).isEqualTo(Train.TrainType.SHATABDI);
        assertThat(dto.getTotalSeats()).isEqualTo(200);
        assertThat(dto.getAvailableSeats()).isEqualTo(150);
    }
}
