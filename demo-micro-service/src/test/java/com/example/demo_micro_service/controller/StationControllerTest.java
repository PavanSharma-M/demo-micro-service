package com.example.demo_micro_service.controller;

import com.example.demo_micro_service.dto.StationDTO;
import com.example.demo_micro_service.exception.DuplicateResourceException;
import com.example.demo_micro_service.exception.ResourceNotFoundException;
import com.example.demo_micro_service.service.StationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StationController.class)
class StationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationService stationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StationDTO buildDTO(Long id, String code, String name) {
        return StationDTO.builder()
                .id(id)
                .stationCode(code)
                .stationName(name)
                .city("Mumbai")
                .state("MH")
                .build();
    }

    // ---- GET /api/v1/stations ----

    @Test
    void getAllStations_returns200WithList() throws Exception {
        given(stationService.getAllStations()).willReturn(
                List.of(buildDTO(1L, "MMCT", "Mumbai Central")));

        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stationCode").value("MMCT"))
                .andExpect(jsonPath("$[0].stationName").value("Mumbai Central"));
    }

    @Test
    void getAllStations_empty_returns200WithEmptyArray() throws Exception {
        given(stationService.getAllStations()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---- GET /api/v1/stations/{id} ----

    @Test
    void getStationById_existingId_returns200() throws Exception {
        given(stationService.getStationById(1L)).willReturn(buildDTO(1L, "MMCT", "Mumbai Central"));

        mockMvc.perform(get("/api/v1/stations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stationCode").value("MMCT"));
    }

    @Test
    void getStationById_notFound_returns404() throws Exception {
        given(stationService.getStationById(99L))
                .willThrow(new ResourceNotFoundException("Station not found with id: 99"));

        mockMvc.perform(get("/api/v1/stations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---- GET /api/v1/stations/code/{stationCode} ----

    @Test
    void getStationByCode_existing_returns200() throws Exception {
        given(stationService.getStationByCode("MMCT")).willReturn(buildDTO(1L, "MMCT", "Mumbai Central"));

        mockMvc.perform(get("/api/v1/stations/code/MMCT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationCode").value("MMCT"));
    }

    @Test
    void getStationByCode_notFound_returns404() throws Exception {
        given(stationService.getStationByCode("UNKNOWN"))
                .willThrow(new ResourceNotFoundException("Station not found with code: UNKNOWN"));

        mockMvc.perform(get("/api/v1/stations/code/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/stations ----

    @Test
    void createStation_validRequest_returns201() throws Exception {
        StationDTO dto = buildDTO(null, "MMCT", "Mumbai Central");
        StationDTO saved = buildDTO(1L, "MMCT", "Mumbai Central");
        given(stationService.createStation(any(StationDTO.class))).willReturn(saved);

        mockMvc.perform(post("/api/v1/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stationCode").value("MMCT"));
    }

    @Test
    void createStation_duplicateCode_returns409() throws Exception {
        StationDTO dto = buildDTO(null, "MMCT", "Mumbai");
        given(stationService.createStation(any(StationDTO.class)))
                .willThrow(new DuplicateResourceException("Station already exists with code: MMCT"));

        mockMvc.perform(post("/api/v1/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createStation_missingCode_returns400() throws Exception {
        // stationCode is blank — @NotBlank validation
        StationDTO invalid = StationDTO.builder()
                .stationCode("")
                .stationName("Mumbai Central")
                .city("Mumbai")
                .state("MH")
                .build();

        mockMvc.perform(post("/api/v1/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/stations/{id} ----

    @Test
    void updateStation_validRequest_returns200() throws Exception {
        StationDTO dto = buildDTO(null, "MMCT", "Updated Name");
        StationDTO updated = buildDTO(1L, "MMCT", "Updated Name");
        given(stationService.updateStation(eq(1L), any(StationDTO.class))).willReturn(updated);

        mockMvc.perform(put("/api/v1/stations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationName").value("Updated Name"));
    }

    @Test
    void updateStation_notFound_returns404() throws Exception {
        StationDTO dto = buildDTO(null, "MMCT", "Mumbai");
        given(stationService.updateStation(eq(99L), any(StationDTO.class)))
                .willThrow(new ResourceNotFoundException("Station not found with id: 99"));

        mockMvc.perform(put("/api/v1/stations/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/v1/stations/{id} ----

    @Test
    void deleteStation_existingId_returns204() throws Exception {
        willDoNothing().given(stationService).deleteStation(1L);

        mockMvc.perform(delete("/api/v1/stations/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteStation_notFound_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("Station not found with id: 99"))
                .given(stationService).deleteStation(99L);

        mockMvc.perform(delete("/api/v1/stations/99"))
                .andExpect(status().isNotFound());
    }
}
