package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ActeurPastoralRequest;
import com.eyram.dev.church_project_spring.DTO.request.ActeurPastoralStatutRequest;
import com.eyram.dev.church_project_spring.DTO.response.ActeurPastoralResponse;
import com.eyram.dev.church_project_spring.service.ActeurPastoralService;
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
@RequestMapping("/acteurs-pastoraux")
@RequiredArgsConstructor
public class ActeurPastoralController {

    private final ActeurPastoralService acteurPastoralService;

    @PostMapping
    @PreAuthorize("hasAuthority('pastoral-actor:manage')")
    public ResponseEntity<ActeurPastoralResponse> create(
            @Valid @RequestBody ActeurPastoralRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(acteurPastoralService.create(request));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-actor:read')")
    public ResponseEntity<ActeurPastoralResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(acteurPastoralService.getByPublicId(publicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}")
    @PreAuthorize("hasAuthority('pastoral-actor:read')")
    public ResponseEntity<List<ActeurPastoralResponse>> getByParoisse(
            @PathVariable UUID paroissePublicId,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false, name = "q") String recherche
    ) {
        return ResponseEntity.ok(
                acteurPastoralService.getByParoisse(paroissePublicId, actif, recherche)
        );
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-actor:manage')")
    public ResponseEntity<ActeurPastoralResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody ActeurPastoralRequest request
    ) {
        return ResponseEntity.ok(acteurPastoralService.update(publicId, request));
    }

    @PostMapping("/{publicId}/statut")
    @PreAuthorize("hasAuthority('pastoral-actor:manage')")
    public ResponseEntity<ActeurPastoralResponse> setActif(
            @PathVariable UUID publicId,
            @Valid @RequestBody ActeurPastoralStatutRequest request
    ) {
        return ResponseEntity.ok(
                acteurPastoralService.setActif(publicId, request.actif(), request.version())
        );
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-actor:manage')")
    public ResponseEntity<Void> archive(
            @PathVariable UUID publicId,
            @RequestParam Long version
    ) {
        acteurPastoralService.archive(publicId, version);
        return ResponseEntity.noContent().build();
    }
}
