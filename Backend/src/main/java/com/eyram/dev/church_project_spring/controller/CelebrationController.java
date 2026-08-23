package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.response.CelebrationIntentionResponse;
import com.eyram.dev.church_project_spring.DTO.response.CelebrationMesseGroupResponse;
import com.eyram.dev.church_project_spring.service.DemandeCelebrationLifecycleService;
import com.eyram.dev.church_project_spring.service.FeuilleCelebrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/celebrations")
@RequiredArgsConstructor
public class CelebrationController {

    private final FeuilleCelebrationService feuilleCelebrationService;
    private final DemandeCelebrationLifecycleService celebrationLifecycleService;

    /**
     * @param heures heures de messe retenues (« HH:mm »), vide = toute la journée
     */
    @GetMapping("/paroisse/{paroissePublicId}")
    public List<CelebrationMesseGroupResponse> listByDate(
            @PathVariable UUID paroissePublicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean inclureNonPayees,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm") List<LocalTime> heures
    ) {
        return feuilleCelebrationService.listByParoisseAndDate(
                paroissePublicId, date, inclureNonPayees, heures
        );
    }

    @GetMapping(value = { "/paroisse/{paroissePublicId}/feuille", "/paroisse/{paroissePublicId}/feuille.pdf" },
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadFeuille(
            @PathVariable UUID paroissePublicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean inclureNonPayees,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm") List<LocalTime> heures
    ) {
        byte[] pdf = feuilleCelebrationService.generatePdf(paroissePublicId, date, inclureNonPayees, heures);
        String filename = "feuille-intentions-" + date + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Confirmation manuelle (sacristain / admin) qu'une intention a été célébrée. */
    @PostMapping("/dates/{demandeDatePublicId}/marquer-celebree")
    public CelebrationIntentionResponse markCelebrated(@PathVariable UUID demandeDatePublicId) {
        return celebrationLifecycleService.markCelebrated(demandeDatePublicId);
    }
}
