package com.example.demo_micro_service.controller;

import com.example.demo_micro_service.dto.TrainDTO;
import com.example.demo_micro_service.exception.DuplicateResourceException;
import com.example.demo_micro_service.exception.ResourceNotFoundException;
import com.example.demo_micro_service.model.Train;
import com.example.demo_micro_service.service.TrainService;
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

@WebMvcTest(TrainController.class)
class TrainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrainService trainService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TrainDTO buildDTO(Long id, String number, String name) {
        return TrainDTO.builder()
                .id(id)
                .trainNumber(number)
                .trainName(name)
                .trainType(Train.TrainType.EXPRESS)
                .totalSeats(100)
                .availableSeats(80)
                .build();
    }

    // ---- GET /api/v1/trains ----

    @Test
    void getAllTrains_returns200WithList() throws Exception {
        given(trainService.getAllTrains()).willReturn(List.of(buildDTO(1L, "12345", "Express")));

        mockMvc.perform(get("/api/v1/trains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainNumber").value("12345"))
                .andExpect(jsonPath("$[0].trainName").value("Express"));
    }

    @Test
    void getAllTrains_empty_returns200WithEmptyArray() throws Exception {
        given(trainService.getAllTrains()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/trains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---- GET /api/v1/trains/{id} ----

    @Test
    void getTrainById_existingId_returns200() throws Exception {
        given(trainService.getTrainById(1L)).willReturn(buildDTO(1L, "12345", "Express"));

        mockMvc.perform(get("/api/v1/trains/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.trainNumber").value("12345"));
    }

    @Test
    void getTrainById_notFound_returns404() throws Exception {
        given(trainService.getTrainById(99L)).willThrow(new ResourceNotFoundException("Train not found with id: 99"));

        mockMvc.perform(get("/api/v1/trains/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Train not found with id: 99"));
    }

    // ---- GET /api/v1/trains/number/{trainNumber} ----

    @Test
    void getTrainByNumber_existing_returns200() throws Exception {
        given(trainService.getTrainByNumber("12345")).willReturn(buildDTO(1L, "12345", "Express"));

        mockMvc.perform(get("/api/v1/trains/number/12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainNumber").value("12345"));
    }

    // ---- POST /api/v1/trains ----

    @Test
    void createTrain_validRequest_returns201() throws Exception {
        TrainDTO dto = buildDTO(null, "12345", "Express");
        TrainDTO saved = buildDTO(1L, "12345", "Express");
        given(trainService.createTrain(any(TrainDTO.class))).willReturn(saved);

        mockMvc.perform(post("/api/v1/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.trainNumber").value("12345"));
    }

    @Test
    void createTrain_duplicateNumber_returns409() throws Exception {
        TrainDTO dto = buildDTO(null, "12345", "Express");
        given(trainService.createTrain(any(TrainDTO.class)))
                .willThrow(new DuplicateResourceException("Train already exists with number: 12345"));

        mockMvc.perform(post("/api/v1/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createTrain_missingTrainNumber_returns400() throws Exception {
        // trainNumber is blank — should trigger @NotBlank validation
        TrainDTO invalid = TrainDTO.builder()
                .trainNumber("")
                .trainName("Express")
                .trainType(Train.TrainType.EXPRESS)
                .totalSeats(100)
                .build();

        mockMvc.perform(post("/api/v1/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/trains/{id} ----

    @Test
    void updateTrain_validRequest_returns200() throws Exception {
        TrainDTO dto = buildDTO(null, "12345", "Updated Name");
        TrainDTO updated = buildDTO(1L, "12345", "Updated Name");
        given(trainService.updateTrain(eq(1L), any(TrainDTO.class))).willReturn(updated);

        mockMvc.perform(put("/api/v1/trains/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainName").value("Updated Name"));
    }

    @Test
    void updateTrain_notFound_returns404() throws Exception {
        TrainDTO dto = buildDTO(null, "12345", "Express");
        given(trainService.updateTrain(eq(99L), any(TrainDTO.class)))
                .willThrow(new ResourceNotFoundException("Train not found with id: 99"));

        mockMvc.perform(put("/api/v1/trains/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/v1/trains/{id} ----

    @Test
    void deleteTrain_existingId_returns204() throws Exception {
        willDoNothing().given(trainService).deleteTrain(1L);

        mockMvc.perform(delete("/api/v1/trains/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTrain_notFound_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("Train not found with id: 99"))
                .given(trainService).deleteTrain(99L);

        mockMvc.perform(delete("/api/v1/trains/99"))
                .andExpect(status().isNotFound());
    }
}
