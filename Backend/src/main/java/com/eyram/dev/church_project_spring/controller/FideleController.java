package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.FideleRequest;
import com.eyram.dev.church_project_spring.DTO.response.FideleResponse;
import com.eyram.dev.church_project_spring.service.FideleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/fideles")
public class FideleController {

    private final FideleService fideleService;

    public FideleController(FideleService fideleService) {
        this.fideleService = fideleService;
    }

    @PostMapping
    public ResponseEntity<FideleResponse> create(@Valid @RequestBody FideleRequest client) {
        FideleResponse response = fideleService.create(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/search/by-name/{nom}")
    public ResponseEntity<List<FideleResponse>> getByName(@PathVariable String nom) {
        List<FideleResponse> clients = fideleService.getNom(nom);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/search/by-id/{publicId}")
    public ResponseEntity<FideleResponse> getByPublicId(@PathVariable UUID publicId) {
        FideleResponse client = fideleService.getByPublicId(publicId);
        return ResponseEntity.ok(client);
    }

    @GetMapping
    public ResponseEntity<List<FideleResponse>> getAll() {
        return ResponseEntity.ok(fideleService.getAll());
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<FideleResponse> update(@PathVariable UUID publicId,
                                                 @Valid @RequestBody FideleRequest client) {
        FideleResponse response = fideleService.update(publicId, client);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID publicId) {
        fideleService.softDelete(publicId);
        return ResponseEntity.ok(Map.of("message", "Fidele supprimé"));
    }

    @GetMapping("/active")
    public ResponseEntity<?> getInactive() {
        List<FideleResponse> inactifs = fideleService.getInactive();
        if (inactifs.isEmpty()) {
            return ResponseEntity.ok("Aucun client inactif trouvé");
        }
        return ResponseEntity.ok(inactifs);
    }

    //supp=false



}
