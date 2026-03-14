package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAccessRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseAccessResponse;
import com.eyram.dev.church_project_spring.service.ParoisseAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/paroisse-access")
@RequiredArgsConstructor
public class ParoisseAccessController {

    private final ParoisseAccessService paroisseAccessService;

    @PostMapping
    public ResponseEntity<ParoisseAccessResponse> create(@Valid @RequestBody ParoisseAccessRequest request) {
        ParoisseAccessResponse response = paroisseAccessService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ParoisseAccessResponse>> getAll() {
        return ResponseEntity.ok(paroisseAccessService.getAll());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<ParoisseAccessResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(paroisseAccessService.getByPublicId(publicId));
    }

    @GetMapping("/user/{userPublicId}")
    public ResponseEntity<List<ParoisseAccessResponse>> getByUser(@PathVariable UUID userPublicId) {
        return ResponseEntity.ok(paroisseAccessService.getByUser(userPublicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}")
    public ResponseEntity<List<ParoisseAccessResponse>> getByParoisse(@PathVariable UUID paroissePublicId) {
        return ResponseEntity.ok(paroisseAccessService.getByParoisse(paroissePublicId));
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<ParoisseAccessResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody ParoisseAccessRequest request
    ) {
        return ResponseEntity.ok(paroisseAccessService.update(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        paroisseAccessService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}