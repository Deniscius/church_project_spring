package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.response.UpcomingCelebrationResponse;
import com.eyram.dev.church_project_spring.service.DashboardProgrammeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardProgrammeService dashboardProgrammeService;

    @GetMapping("/paroisses/{paroissePublicId}/programmations")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETAIRE', 'CURE', 'COMPTABLE_LOCAL')")
    public ResponseEntity<List<UpcomingCelebrationResponse>> upcomingCelebrations(
            @PathVariable UUID paroissePublicId,
            @RequestParam(defaultValue = "14") int jours
    ) {
        return ResponseEntity.ok(dashboardProgrammeService.findUpcoming(paroissePublicId, jours));
    }
}
