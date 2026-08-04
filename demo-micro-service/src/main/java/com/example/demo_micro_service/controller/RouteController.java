package com.example.demo_micro_service.controller;

import com.example.demo_micro_service.dto.RouteDTO;
import com.example.demo_micro_service.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Route Management", description = "APIs for managing train routes and schedules")
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    @Operation(summary = "Get all routes")
    public ResponseEntity<List<RouteDTO>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get route by ID")
    public ResponseEntity<RouteDTO> getRouteById(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getRouteById(id));
    }

    @GetMapping("/train/{trainId}")
    @Operation(summary = "Get routes by train ID")
    public ResponseEntity<List<RouteDTO>> getRoutesByTrain(@PathVariable Long trainId) {
        return ResponseEntity.ok(routeService.getRoutesByTrain(trainId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search routes by source and destination station codes")
    public ResponseEntity<List<RouteDTO>> searchRoutes(
            @RequestParam String source,
            @RequestParam String destination) {
        return ResponseEntity.ok(routeService.searchRoutes(source, destination));
    }

    @PostMapping
    @Operation(summary = "Create a new route")
    public ResponseEntity<RouteDTO> createRoute(@Valid @RequestBody RouteDTO routeDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.createRoute(routeDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update route details")
    public ResponseEntity<RouteDTO> updateRoute(@PathVariable Long id,
                                                @Valid @RequestBody RouteDTO routeDTO) {
        return ResponseEntity.ok(routeService.updateRoute(id, routeDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a route")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
