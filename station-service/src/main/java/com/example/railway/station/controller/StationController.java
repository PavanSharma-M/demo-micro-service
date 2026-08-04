package com.example.railway.station.controller;

import com.example.railway.station.dto.StationDTO;
import com.example.railway.station.service.StationService;
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
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
@Tag(name = "Station Management", description = "APIs for managing railway stations")
@SecurityRequirement(name = "Keycloak")
public class StationController {

    private final StationService stationService;

    @GetMapping
    @Operation(summary = "Get all stations")
    public ResponseEntity<List<StationDTO>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get station by ID")
    public ResponseEntity<StationDTO> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }

    @GetMapping("/code/{stationCode}")
    @Operation(summary = "Get station by code")
    public ResponseEntity<StationDTO> getStationByCode(@PathVariable String stationCode) {
        return ResponseEntity.ok(stationService.getStationByCode(stationCode));
    }

    @PostMapping
    @Operation(summary = "Create a new station (ADMIN)")
    public ResponseEntity<StationDTO> createStation(@Valid @RequestBody StationDTO stationDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stationService.createStation(stationDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update station details (ADMIN)")
    public ResponseEntity<StationDTO> updateStation(@PathVariable Long id,
                                                    @Valid @RequestBody StationDTO stationDTO) {
        return ResponseEntity.ok(stationService.updateStation(id, stationDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a station (ADMIN)")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }
}
