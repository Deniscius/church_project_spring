package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import com.eyram.dev.church_project_spring.DTO.response.LocaliteResponse;
import com.eyram.dev.church_project_spring.service.LocaliteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/localites")
public class LocaliteController {

    private final LocaliteService localiteService;

    @Autowired
    public LocaliteController(LocaliteService localiteService) {
        this.localiteService = localiteService;
    }


    @PostMapping
    public ResponseEntity<LocaliteResponse> create(@RequestBody LocaliteRequest request) {
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
    public ResponseEntity<LocaliteResponse> update(@PathVariable UUID publicId, @RequestBody LocaliteRequest localiteRequest) {
        LocaliteResponse response = localiteService.update(publicId, localiteRequest);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{publicId}")
    public ResponseEntity<String> delete(@PathVariable UUID publicId) {
        localiteService.delete(publicId);
        return ResponseEntity.ok("Localite supprimée avec succès");
    }
}
