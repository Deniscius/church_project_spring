package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.response.DemandeListItemResponse;
import com.eyram.dev.church_project_spring.DTO.response.PageResponse;
import com.eyram.dev.church_project_spring.service.DemandeAdminSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class DemandeAdminSearchController {

    private final DemandeAdminSearchService demandeAdminSearchService;

    @GetMapping("/paroisse/{paroissePublicId}/recherche")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETAIRE', 'CURE', 'COMPTABLE_LOCAL', 'COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<PageResponse<DemandeListItemResponse>> searchByParoisse(
            @PathVariable UUID paroissePublicId,
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        return ResponseEntity.ok(
                demandeAdminSearchService.search(
                        paroissePublicId,
                        query,
                        page,
                        size,
                        includeDeleted
                )
        );
    }
}
