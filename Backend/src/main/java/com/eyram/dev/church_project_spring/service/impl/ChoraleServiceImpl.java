package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ChoraleRequest;
import com.eyram.dev.church_project_spring.DTO.response.ChoraleClaimResponse;
import com.eyram.dev.church_project_spring.DTO.response.ChoraleInvitationResponse;
import com.eyram.dev.church_project_spring.DTO.response.ChoraleResponse;
import com.eyram.dev.church_project_spring.entities.Chorale;
import com.eyram.dev.church_project_spring.entities.ChoraleInvitation;
import com.eyram.dev.church_project_spring.entities.Doyenne;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.repositories.ChoraleInvitationRepository;
import com.eyram.dev.church_project_spring.repositories.ChoraleRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.ChoraleService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChoraleServiceImpl implements ChoraleService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ChoraleRepository choraleRepository;
    private final ChoraleInvitationRepository invitationRepository;
    private final ParoisseRepository paroisseRepository;
    private final TenantAccessService tenantAccessService;

    @Override
    public ChoraleResponse create(UUID paroissePublicId, ChoraleRequest request) {
        Paroisse paroisse = parish(paroissePublicId);
        tenantAccessService.checkParoisseAccess(paroisse);
        String nom = normalizeName(request.nom());
        if (choraleRepository.existsByNomIgnoreCaseAndParoisse_PublicIdAndStatusDelFalse(nom, paroissePublicId)) {
            throw new AlreadyExistException("Cette chorale existe déjà dans cette paroisse");
        }
        Chorale chorale = new Chorale();
        chorale.setPublicId(UUID.randomUUID());
        chorale.setNom(nom);
        chorale.setDescription(normalizeOptional(request.description()));
        chorale.setIsActive(true);
        chorale.setStatusDel(false);
        chorale.setParoisse(paroisse);
        return response(choraleRepository.save(chorale));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChoraleResponse> list(UUID paroissePublicId) {
        Paroisse paroisse = parish(paroissePublicId);
        tenantAccessService.checkParoisseAccess(paroisse);
        return choraleRepository.findAllByParoisse_PublicIdAndStatusDelFalseOrderByNomAsc(paroissePublicId)
                .stream().map(this::response).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChoraleResponse get(UUID publicId) {
        Chorale chorale = choir(publicId);
        tenantAccessService.checkParoisseAccess(chorale.getParoisse());
        return response(chorale);
    }

    @Override
    public ChoraleResponse update(UUID publicId, ChoraleRequest request) {
        Chorale chorale = choir(publicId);
        tenantAccessService.checkParoisseAccess(chorale.getParoisse());
        String nom = normalizeName(request.nom());
        if (choraleRepository.existsByNomIgnoreCaseAndParoisse_PublicIdAndStatusDelFalseAndPublicIdNot(
                nom, chorale.getParoisse().getPublicId(), publicId)) {
            throw new AlreadyExistException("Cette chorale existe déjà dans cette paroisse");
        }
        chorale.setNom(nom);
        chorale.setDescription(normalizeOptional(request.description()));
        return response(choraleRepository.save(chorale));
    }

    @Override
    public void delete(UUID publicId) {
        Chorale chorale = choir(publicId);
        tenantAccessService.checkParoisseAccess(chorale.getParoisse());
        chorale.setIsActive(false);
        chorale.setStatusDel(true);
        choraleRepository.save(chorale);
    }

    @Override
    public ChoraleInvitationResponse invite(UUID publicId) {
        Chorale chorale = choir(publicId);
        tenantAccessService.checkParoisseAccess(chorale.getParoisse());
        if (!Boolean.TRUE.equals(chorale.getIsActive())) {
            throw new BusinessRuleException("Une chorale inactive ne peut pas recevoir d'invitation");
        }
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = "choir_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ChoraleInvitation invitation = new ChoraleInvitation();
        invitation.setPublicId(UUID.randomUUID());
        invitation.setChorale(chorale);
        invitation.setTokenHash(hash(token));
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation.setStatusDel(false);
        invitationRepository.save(invitation);
        return new ChoraleInvitationResponse(invitation.getPublicId(), token, invitation.getExpiresAt());
    }

    @Override
    @Transactional(readOnly = true)
    public ChoraleClaimResponse resolveInvitation(String token) {
        return claim(activeInvitation(token).getChorale());
    }

    @Override
    public ChoraleClaimResponse consumeInvitation(String token) {
        requireTokenFormat(token);
        ChoraleInvitation invitation = invitationRepository.findForUpdateByTokenHash(hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("Invitation invalide ou expirée"));
        assertActive(invitation);
        invitation.setUsedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
        return claim(invitation.getChorale());
    }

    private ChoraleInvitation activeInvitation(String raw) {
        requireTokenFormat(raw);
        ChoraleInvitation invitation = invitationRepository.findByTokenHashAndStatusDelFalse(hash(raw))
                .orElseThrow(() -> new ResourceNotFoundException("Invitation invalide ou expirée"));
        assertActive(invitation);
        return invitation;
    }

    private void assertActive(ChoraleInvitation invitation) {
        if (invitation.getUsedAt() != null || invitation.getExpiresAt().isBefore(LocalDateTime.now())
                || !Boolean.TRUE.equals(invitation.getChorale().getIsActive())
                || Boolean.TRUE.equals(invitation.getChorale().getStatusDel())) {
            throw new ResourceNotFoundException("Invitation invalide ou expirée");
        }
    }

    private void requireTokenFormat(String raw) {
        if (raw == null || !raw.startsWith("choir_") || raw.length() < 40) {
            throw new ResourceNotFoundException("Invitation invalide ou expirée");
        }
    }

    private Chorale choir(UUID publicId) {
        return choraleRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Chorale non trouvée"));
    }

    private Paroisse parish(UUID publicId) {
        return paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse non trouvée"));
    }

    private ChoraleResponse response(Chorale c) {
        Paroisse p = c.getParoisse();
        Doyenne d = p.getDoyenne();
        return new ChoraleResponse(c.getPublicId(), c.getNom(), c.getDescription(), c.getIsActive(),
                p.getPublicId(), p.getNom(), d.getPublicId(), d.getNom(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private ChoraleClaimResponse claim(Chorale c) {
        Paroisse p = c.getParoisse();
        Doyenne d = p.getDoyenne();
        return new ChoraleClaimResponse(c.getPublicId(), c.getNom(), p.getPublicId(), p.getNom(), d.getPublicId(), d.getNom());
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Le nom est obligatoire");
        return value.strip().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip().replaceAll("\\s+", " ");
    }

    private String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
