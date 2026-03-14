package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import com.eyram.dev.church_project_spring.DTO.response.LocaliteResponse;
import com.eyram.dev.church_project_spring.service.LocaliteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/localites")
@RequiredArgsConstructor
public class LocaliteController {

    private final LocaliteService localiteService;

    @PostMapping
    public ResponseEntity<LocaliteResponse> create(@Valid @RequestBody LocaliteRequest request) {
        LocaliteResponse response = localiteService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<LocaliteResponse> getByPublicId(@PathVariable UUID publicId) {
        LocaliteResponse response = localiteService.getByPublicId(publicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<LocaliteResponse>> getAll() {
        List<LocaliteResponse> localites = localiteService.getAll();
        return ResponseEntity.ok(localites);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<LocaliteResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody LocaliteRequest request
    ) {
        LocaliteResponse response = localiteService.update(publicId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deleteByPublicId(@PathVariable UUID publicId) {
        localiteService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}