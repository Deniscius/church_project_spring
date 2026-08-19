package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.response.DemandeListItemResponse;
import com.eyram.dev.church_project_spring.DTO.response.PageResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.DemandeAdminSearchService;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemandeAdminSearchServiceImpl implements DemandeAdminSearchService {

    private static final int MAX_PAGE_SIZE = 100;

    private final DemandeRepository demandeRepository;
    private final DemandeDateRepository demandeDateRepository;
    private final ParoisseRepository paroisseRepository;
    private final TenantAccessService tenantAccessService;

    @Override
    public PageResponse<DemandeListItemResponse> search(
            UUID paroissePublicId,
            String query,
            int page,
            int size,
            boolean includeDeleted
    ) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        tenantAccessService.checkParoisseAccess(paroisse);

        if (includeDeleted) {
            tenantAccessService.requireIncludeDeletedDemandes();
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        if (!StringUtils.hasText(query)) {
            return PageResponse.of(List.of(), safePage, safeSize, 0);
        }

        String normalizedText = query.trim()
                .toLowerCase(Locale.ROOT)
                .replace("%", "")
                .replace("_", "");
        String digits = query.replaceAll("\\D", "");

        String textPattern = "%" + normalizedText + "%";
        String phonePattern = digits.length() >= 4
                ? "%" + digits + "%"
                : "#NO_PHONE_MATCH#";

        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Demande> result = demandeRepository.searchByParoisse(
                paroisse,
                includeDeleted,
                textPattern,
                phonePattern,
                pageable
        );

        List<Long> demandeIds = result.getContent().stream()
                .map(Demande::getId)
                .toList();
        Map<Long, List<LocalDate>> datesByDemande = demandeIds.isEmpty()
                ? Map.of()
                : demandeDateRepository.findByDemande_IdInAndStatusDelFalseOrderByOrdreAsc(demandeIds)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> row.getDemande().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(DemandeDate::getDateCelebration, Collectors.toList())
                ));

        List<DemandeListItemResponse> content = result.getContent().stream()
                .map(demande -> new DemandeListItemResponse(
                        demande.getPublicId(),
                        demande.getCodeSuivie(),
                        demande.getNomFidele(),
                        demande.getPrenomFidele(),
                        demande.getTelFidele(),
                        demande.getTypeDemande() != null ? demande.getTypeDemande().getLibelle() : null,
                        demande.getStatutDemande(),
                        demande.getStatutValidation(),
                        demande.getStatutPaiement(),
                        demande.getMontant(),
                        demande.getStatusDel(),
                        demande.getDeletedAt(),
                        demande.getDeletedByNom(),
                        demande.getCreatedAt(),
                        datesByDemande.getOrDefault(demande.getId(), List.of())
                ))
                .toList();

        return PageResponse.of(content, safePage, safeSize, result.getTotalElements());
    }
}
