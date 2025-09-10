package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.service.DetailsPaiementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/details-paiements")
@RequiredArgsConstructor
public class DetailsPaiementController {

    private final DetailsPaiementService service;

    // ✅ Créer un paiement
    @PostMapping
    public ResponseEntity<DetailsPaiement> create(@RequestBody DetailsPaiement detailsPaiement) {
        return ResponseEntity.ok(service.create(detailsPaiement));
    }

    // ✅ Récupérer par publicId
    @GetMapping("/{publicId}")
    public ResponseEntity<DetailsPaiement> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    // ✅ Récupérer tous
    @GetMapping
    public ResponseEntity<List<DetailsPaiement>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // ✅ Mettre à jour un paiement
    @PutMapping("/{publicId}")
    public ResponseEntity<DetailsPaiement> update(@PathVariable UUID publicId,
                                                  @RequestBody DetailsPaiement detailsPaiement) {
        return ResponseEntity.ok(service.update(publicId, detailsPaiement));
    }

    // ✅ Supprimer un paiement
    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        service.delete(publicId);
        return ResponseEntity.noContent().build();
    }

    // ✅ Recherche par date
    @GetMapping("/date/{date}")
    public ResponseEntity<DetailsPaiement> getByDate(@PathVariable String date) {
        LocalDate parsedDate = LocalDate.parse(date); // format "yyyy-MM-dd"
        return ResponseEntity.ok(((com.eyram.dev.church_project_spring.service.impl.DetailsPaiementServiceImpl) service).getByDate(parsedDate));
    }

    // ✅ Recherche par montant
    @GetMapping("/montant/{montant}")
    public ResponseEntity<DetailsPaiement> getByMontant(@PathVariable Double montant) {
        return ResponseEntity.ok(((com.eyram.dev.church_project_spring.service.impl.DetailsPaiementServiceImpl) service).getByMontant(montant));
    }

    // ✅ Recherche par statut (statusDel = true/false)
    @GetMapping("/status/{statusDel}")
    public ResponseEntity<List<DetailsPaiement>> getByStatus(@PathVariable Boolean statusDel) {
        return ResponseEntity.ok(((com.eyram.dev.church_project_spring.service.impl.DetailsPaiementServiceImpl) service).getByStatus(statusDel));
    }
}
