package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DoyenneRequest;
import com.eyram.dev.church_project_spring.DTO.response.DoyenneResponse;
import com.eyram.dev.church_project_spring.service.DoyenneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/doyennes")
@RequiredArgsConstructor
@Validated
public class DoyenneController {

    private final DoyenneService doyenneService;

    @PostMapping
    public ResponseEntity<DoyenneResponse> create(@Valid @RequestBody DoyenneRequest request) {
        DoyenneResponse response = doyenneService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<DoyenneResponse> getByPublicId(@PathVariable UUID publicId) {
        DoyenneResponse response = doyenneService.getByPublicId(publicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DoyenneResponse>> getAll() {
        List<DoyenneResponse> doyennes = doyenneService.getAll();
        return ResponseEntity.ok(doyennes);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<DoyenneResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody DoyenneRequest request
    ) {
        DoyenneResponse response = doyenneService.update(publicId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deleteByPublicId(@PathVariable UUID publicId) {
        doyenneService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}
