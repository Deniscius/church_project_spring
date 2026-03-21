package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemandeRepository extends JpaRepository<Demande, Long> {

    Optional<Demande> findByPublicIdAndStatusDelFalse(UUID publicId);

    Optional<Demande> findByCodeSuivieAndStatusDelFalse(String codeSuivie);

    List<Demande> findByStatusDelFalse();

    List<Demande> findByParoisseAndStatusDelFalse(Paroisse paroisse);
    List<Demande> findByTypePaiementPublicIdAndStatusDelFalse(UUID typePaiementPublicId);
    List<Demande> findByParoisseAndStatutDemandeAndStatusDelFalse(Paroisse paroisse, StatutDemandeEnum statutDemande);

    boolean existsByCodeSuivieAndStatusDelFalse(String codeSuivie);
}