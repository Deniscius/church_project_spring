package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.CelebrationScheduleUpdateRequest;
import com.eyram.dev.church_project_spring.DTO.response.UpcomingCelebrationResponse;
import com.eyram.dev.church_project_spring.service.DashboardProgrammeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardProgrammeService dashboardProgrammeService;

    @GetMapping("/paroisses/{paroissePublicId}/programmations")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ResponseEntity<List<UpcomingCelebrationResponse>> upcomingCelebrations(
            @PathVariable UUID paroissePublicId,
            @RequestParam(defaultValue = "14") int jours
    ) {
        return ResponseEntity.ok(dashboardProgrammeService.findUpcoming(paroissePublicId, jours));
    }

    @GetMapping("/paroisses/{paroissePublicId}/programmations/passees")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ResponseEntity<List<UpcomingCelebrationResponse>> pastCelebrations(
            @PathVariable UUID paroissePublicId,
            @RequestParam(defaultValue = "14") int jours
    ) {
        return ResponseEntity.ok(dashboardProgrammeService.findPast(paroissePublicId, jours));
    }

    @GetMapping("/paroisses/{paroissePublicId}/programmations/jour/{date}")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ResponseEntity<List<UpcomingCelebrationResponse>> celebrationsForDay(
            @PathVariable UUID paroissePublicId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(dashboardProgrammeService.findByDate(paroissePublicId, date));
    }

    @PatchMapping("/paroisses/{paroissePublicId}/programmations/{demandeDatePublicId}/horaire")
    @PreAuthorize("hasAuthority('celebration-schedule:manage')")
    public ResponseEntity<UpcomingCelebrationResponse> updateCelebrationSchedule(
            @PathVariable UUID paroissePublicId,
            @PathVariable UUID demandeDatePublicId,
            @Valid @RequestBody CelebrationScheduleUpdateRequest request
    ) {
        return ResponseEntity.ok(
                dashboardProgrammeService.updateSchedule(
                        paroissePublicId,
                        demandeDatePublicId,
                        request.horairePublicId()
                )
        );
    }
}
