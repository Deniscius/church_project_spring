package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.AnneePastoraleRequest;
import com.eyram.dev.church_project_spring.DTO.request.AnneePastoraleTransitionRequest;
import com.eyram.dev.church_project_spring.DTO.response.AnneePastoraleResponse;
import com.eyram.dev.church_project_spring.service.AnneePastoraleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/annees-pastorales")
@RequiredArgsConstructor
public class AnneePastoraleController {

    private final AnneePastoraleService anneePastoraleService;

    @PostMapping
    @PreAuthorize("hasAuthority('pastoral-year:manage')")
    public ResponseEntity<AnneePastoraleResponse> create(
            @Valid @RequestBody AnneePastoraleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(anneePastoraleService.create(request));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-year:read')")
    public ResponseEntity<AnneePastoraleResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(anneePastoraleService.getByPublicId(publicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}")
    @PreAuthorize("hasAuthority('pastoral-year:read')")
    public ResponseEntity<List<AnneePastoraleResponse>> getByParoisse(
            @PathVariable UUID paroissePublicId
    ) {
        return ResponseEntity.ok(anneePastoraleService.getByParoisse(paroissePublicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}/publiee")
    @PreAuthorize("hasAuthority('pastoral-year:read')")
    public ResponseEntity<AnneePastoraleResponse> getPublishedByParoisse(
            @PathVariable UUID paroissePublicId
    ) {
        return ResponseEntity.ok(anneePastoraleService.getPublishedByParoisse(paroissePublicId));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-year:manage')")
    public ResponseEntity<AnneePastoraleResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody AnneePastoraleRequest request
    ) {
        return ResponseEntity.ok(anneePastoraleService.update(publicId, request));
    }

    @PostMapping("/{publicId}/publication")
    @PreAuthorize("hasAuthority('pastoral-year:manage')")
    public ResponseEntity<AnneePastoraleResponse> publish(
            @PathVariable UUID publicId,
            @Valid @RequestBody AnneePastoraleTransitionRequest request
    ) {
        return ResponseEntity.ok(anneePastoraleService.publish(publicId, request.version()));
    }

    @PostMapping("/{publicId}/cloture")
    @PreAuthorize("hasAuthority('pastoral-year:manage')")
    public ResponseEntity<AnneePastoraleResponse> close(
            @PathVariable UUID publicId,
            @Valid @RequestBody AnneePastoraleTransitionRequest request
    ) {
        return ResponseEntity.ok(anneePastoraleService.close(publicId, request.version()));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-year:manage')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID publicId,
            @RequestParam Long version
    ) {
        anneePastoraleService.deleteByPublicId(publicId, version);
        return ResponseEntity.noContent().build();
    }
}
