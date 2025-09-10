package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.service.ParoisseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/paroisses")
public class ParoisseController {

    private final ParoisseService paroisseService;

    public ParoisseController(ParoisseService paroisseService) {
        this.paroisseService = paroisseService;
    }


    @PostMapping
    public ResponseEntity<ParoisseResponse> create(@Valid @RequestBody ParoisseRequest paroisseRequest) {
        ParoisseResponse response = paroisseService.create(paroisseRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    @GetMapping("/{publicId}")
    public ResponseEntity<ParoisseResponse> getById(@PathVariable UUID publicId) {
        ParoisseResponse response = paroisseService.getByPublicId(publicId);
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<List<ParoisseResponse>> listAll() {
        List<ParoisseResponse> paroisses = paroisseService.list();
        return ResponseEntity.ok(paroisses);
    }


    @PutMapping("/{publicId}")
    public ResponseEntity<ParoisseResponse> update(@PathVariable UUID publicId,
                                                   @Valid @RequestBody ParoisseRequest paroisseRequest) {
        ParoisseResponse response = paroisseService.update(paroisseRequest);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{publicId}")
    public ResponseEntity<Map<String, String>> deleteByPublicId(@PathVariable UUID publicId) {
        paroisseService.deleteByPublicId(publicId);
        return ResponseEntity.ok(Map.of("message", "Paroisse supprimée avec succès"));
    }

    @DeleteMapping("/name/{nom}")
    public ResponseEntity<Map<String, String>> deleteByName(@PathVariable String nom) {
        paroisseService.deleteByName(nom);
        return ResponseEntity.ok(Map.of("message", "Paroisse supprimée avec succès"));
    }
}
