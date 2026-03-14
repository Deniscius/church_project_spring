package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.service.ParoisseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/paroisses")
@RequiredArgsConstructor
public class ParoisseController {

    private final ParoisseService paroisseService;

    @PostMapping
    public ResponseEntity<ParoisseResponse> create(@Valid @RequestBody ParoisseRequest request) {
        ParoisseResponse response = paroisseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ParoisseResponse>> getAll() {
        List<ParoisseResponse> responses = paroisseService.getAll();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<ParoisseResponse> getByPublicId(@PathVariable UUID publicId) {
        ParoisseResponse response = paroisseService.getByPublicId(publicId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<ParoisseResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody ParoisseRequest request
    ) {
        ParoisseResponse response = paroisseService.update(publicId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deleteByPublicId(@PathVariable UUID publicId) {
        paroisseService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}