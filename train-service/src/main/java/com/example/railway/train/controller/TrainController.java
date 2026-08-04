package com.example.railway.train.controller;

import com.example.railway.train.dto.TrainDTO;
import com.example.railway.train.service.TrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trains")
@RequiredArgsConstructor
@Tag(name = "Train Management", description = "APIs for managing railway trains")
@SecurityRequirement(name = "Keycloak")
public class TrainController {

    private final TrainService trainService;

    @GetMapping
    @Operation(summary = "Get all trains")
    public ResponseEntity<List<TrainDTO>> getAllTrains() {
        return ResponseEntity.ok(trainService.getAllTrains());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get train by ID")
    public ResponseEntity<TrainDTO> getTrainById(@PathVariable Long id) {
        return ResponseEntity.ok(trainService.getTrainById(id));
    }

    @GetMapping("/number/{trainNumber}")
    @Operation(summary = "Get train by number")
    public ResponseEntity<TrainDTO> getTrainByNumber(@PathVariable String trainNumber) {
        return ResponseEntity.ok(trainService.getTrainByNumber(trainNumber));
    }

    @PostMapping
    @Operation(summary = "Create a new train (ADMIN)")
    public ResponseEntity<TrainDTO> createTrain(@Valid @RequestBody TrainDTO trainDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainService.createTrain(trainDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update train details (ADMIN)")
    public ResponseEntity<TrainDTO> updateTrain(@PathVariable Long id,
                                                @Valid @RequestBody TrainDTO trainDTO) {
        return ResponseEntity.ok(trainService.updateTrain(id, trainDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a train (ADMIN)")
    public ResponseEntity<Void> deleteTrain(@PathVariable Long id) {
        trainService.deleteTrain(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Internal endpoint — called by booking-service to reserve or release seats.
     * delta < 0: reserve seats (booking). delta > 0: release seats (cancellation).
     */
    @PatchMapping("/{id}/seats")
    @Operation(summary = "Update available seats (internal — booking-service)")
    public ResponseEntity<TrainDTO> updateSeats(@PathVariable Long id,
                                                @RequestParam int delta) {
        return ResponseEntity.ok(trainService.updateAvailableSeats(id, delta));
    }
}
