package com.example.demo_micro_service.service;

import com.example.demo_micro_service.dto.StationDTO;
import com.example.demo_micro_service.exception.DuplicateResourceException;
import com.example.demo_micro_service.exception.ResourceNotFoundException;
import com.example.demo_micro_service.model.Station;
import com.example.demo_micro_service.repository.StationRepository;
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
class StationServiceTest {

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private StationService stationService;

    private Station buildStation(Long id, String code, String name, String city, String state) {
        return Station.builder()
                .id(id)
                .stationCode(code)
                .stationName(name)
                .city(city)
                .state(state)
                .build();
    }

    // ---- getAllStations ----

    @Test
    void getAllStations_returnsMappedDTOList() {
        given(stationRepository.findAll()).willReturn(
                List.of(buildStation(1L, "MMCT", "Mumbai Central", "Mumbai", "MH")));

        List<StationDTO> result = stationService.getAllStations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStationCode()).isEqualTo("MMCT");
    }

    @Test
    void getAllStations_empty_returnsEmptyList() {
        given(stationRepository.findAll()).willReturn(List.of());

        assertThat(stationService.getAllStations()).isEmpty();
    }

    // ---- getStationById ----

    @Test
    void getStationById_existingId_returnsStation() {
        given(stationRepository.findById(1L))
                .willReturn(Optional.of(buildStation(1L, "MMCT", "Mumbai Central", "Mumbai", "MH")));

        StationDTO dto = stationService.getStationById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getStationCode()).isEqualTo("MMCT");
        assertThat(dto.getCity()).isEqualTo("Mumbai");
    }

    @Test
    void getStationById_nonExistingId_throwsResourceNotFoundException() {
        given(stationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.getStationById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---- getStationByCode ----

    @Test
    void getStationByCode_existingCode_returnsStation() {
        given(stationRepository.findByStationCode("MMCT"))
                .willReturn(Optional.of(buildStation(1L, "MMCT", "Mumbai Central", "Mumbai", "MH")));

        StationDTO dto = stationService.getStationByCode("MMCT");

        assertThat(dto.getStationCode()).isEqualTo("MMCT");
        assertThat(dto.getStationName()).isEqualTo("Mumbai Central");
    }

    @Test
    void getStationByCode_nonExistingCode_throwsResourceNotFoundException() {
        given(stationRepository.findByStationCode("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.getStationByCode("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ---- createStation ----

    @Test
    void createStation_newCode_savesWithUpperCaseCodeAndReturnsDTO() {
        StationDTO dto = StationDTO.builder()
                .stationCode("mmct")
                .stationName("Mumbai Central")
                .city("Mumbai")
                .state("MH")
                .build();
        given(stationRepository.existsByStationCode("mmct")).willReturn(false);
        given(stationRepository.save(any(Station.class))).willAnswer(inv -> {
            Station s = inv.getArgument(0);
            return Station.builder()
                    .id(1L)
                    .stationCode(s.getStationCode())
                    .stationName(s.getStationName())
                    .city(s.getCity())
                    .state(s.getState())
                    .build();
        });

        StationDTO result = stationService.createStation(dto);

        assertThat(result.getId()).isEqualTo(1L);
        // Station code is uppercased by the service
        assertThat(result.getStationCode()).isEqualTo("MMCT");
    }

    @Test
    void createStation_duplicateCode_throwsDuplicateResourceException() {
        StationDTO dto = StationDTO.builder()
                .stationCode("MMCT")
                .stationName("Mumbai")
                .city("Mumbai")
                .state("MH")
                .build();
        given(stationRepository.existsByStationCode("MMCT")).willReturn(true);

        assertThatThrownBy(() -> stationService.createStation(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("MMCT");
    }

    // ---- updateStation ----

    @Test
    void updateStation_sameCode_updatesAllFields() {
        Station existing = buildStation(1L, "MMCT", "Old Name", "Mumbai", "MH");
        StationDTO dto = StationDTO.builder()
                .stationCode("MMCT")
                .stationName("New Name")
                .city("Pune")
                .state("MH")
                .build();

        given(stationRepository.findById(1L)).willReturn(Optional.of(existing));
        given(stationRepository.save(any(Station.class))).willAnswer(inv -> inv.getArgument(0));

        StationDTO result = stationService.updateStation(1L, dto);

        assertThat(result.getStationName()).isEqualTo("New Name");
        assertThat(result.getCity()).isEqualTo("Pune");
    }

    @Test
    void updateStation_changedCodeToDuplicate_throwsDuplicateResourceException() {
        Station existing = buildStation(1L, "MMCT", "Mumbai", "Mumbai", "MH");
        StationDTO dto = StationDTO.builder()
                .stationCode("NDLS")
                .stationName("Delhi")
                .city("Delhi")
                .state("DL")
                .build();

        given(stationRepository.findById(1L)).willReturn(Optional.of(existing));
        given(stationRepository.existsByStationCode("NDLS")).willReturn(true);

        assertThatThrownBy(() -> stationService.updateStation(1L, dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("NDLS");
    }

    @Test
    void updateStation_nonExistingId_throwsResourceNotFoundException() {
        given(stationRepository.findById(99L)).willReturn(Optional.empty());
        StationDTO dto = StationDTO.builder().stationCode("X").stationName("X").city("X").state("X").build();

        assertThatThrownBy(() -> stationService.updateStation(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- deleteStation ----

    @Test
    void deleteStation_existingId_callsDeleteById() {
        given(stationRepository.findById(1L))
                .willReturn(Optional.of(buildStation(1L, "MMCT", "Mumbai", "Mumbai", "MH")));
        willDoNothing().given(stationRepository).deleteById(1L);

        stationService.deleteStation(1L);

        then(stationRepository).should().deleteById(1L);
    }

    @Test
    void deleteStation_nonExistingId_throwsResourceNotFoundException() {
        given(stationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.deleteStation(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- toDTO ----

    @Test
    void toDTO_mapsAllFieldsCorrectly() {
        Station station = buildStation(3L, "NDLS", "New Delhi", "Delhi", "DL");

        StationDTO dto = stationService.toDTO(station);

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getStationCode()).isEqualTo("NDLS");
        assertThat(dto.getStationName()).isEqualTo("New Delhi");
        assertThat(dto.getCity()).isEqualTo("Delhi");
        assertThat(dto.getState()).isEqualTo("DL");
    }
}
