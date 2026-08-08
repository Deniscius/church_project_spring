package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAccessRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseAccessResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.mappers.ParoisseAccessMapper;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.ParoisseAccessService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ParoisseAccessServiceImpl implements ParoisseAccessService {

    private final ParoisseAccessRepository paroisseAccessRepository;
    private final UserRepository userRepository;
    private final ParoisseRepository paroisseRepository;
    private final ParoisseAccessMapper paroisseAccessMapper;

    @Override
    public ParoisseAccessResponse create(ParoisseAccessRequest request) {
        validateRequest(request);

        User user = findEligibleUser(request.userPublicId());
        Paroisse paroisse = findActiveParoisse(request.paroissePublicId());
        validateAccessStateForUser(user, request.active());

        if (paroisseAccessRepository.existsByUserAndParoisseAndStatusDelFalse(user, paroisse)) {
            throw new AlreadyExistException("Cet accès existe déjà pour cet utilisateur et cette paroisse");
        }
        ensureSingleActiveParoisse(user, null, request.active());

        ParoisseAccess paroisseAccess = paroisseAccessMapper.dtoToModel(request);
        paroisseAccess.setUser(user);
        paroisseAccess.setParoisse(paroisse);
        paroisseAccess.setStatusDel(false);

        ParoisseAccess savedParoisseAccess = paroisseAccessRepository.save(paroisseAccess);
        return paroisseAccessMapper.modelToDto(savedParoisseAccess);
    }

    @Override
    public ParoisseAccessResponse update(UUID publicId, ParoisseAccessRequest request) {
        if (publicId == null) {
            throw new BusinessRuleException("L'identifiant de l'accès est obligatoire");
        }
        validateRequest(request);

        ParoisseAccess existingParoisseAccess = paroisseAccessRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Accès paroisse introuvable"));

        User user = findEligibleUser(request.userPublicId());
        Paroisse paroisse = findActiveParoisse(request.paroissePublicId());
        validateAccessStateForUser(user, request.active());

        boolean accessChanged =
                !existingParoisseAccess.getUser().getPublicId().equals(request.userPublicId()) ||
                !existingParoisseAccess.getParoisse().getPublicId().equals(request.paroissePublicId());

        if (accessChanged && paroisseAccessRepository.existsByUserAndParoisseAndStatusDelFalse(user, paroisse)) {
            throw new AlreadyExistException("Cet accès existe déjà pour cet utilisateur et cette paroisse");
        }
        ensureSingleActiveParoisse(user, existingParoisseAccess, request.active());
        ensureActiveUserKeepsAccess(existingParoisseAccess, user, request.active());

        paroisseAccessMapper.updateEntityFromDto(request, existingParoisseAccess);
        existingParoisseAccess.setUser(user);
        existingParoisseAccess.setParoisse(paroisse);

        ParoisseAccess updatedParoisseAccess = paroisseAccessRepository.save(existingParoisseAccess);
        return paroisseAccessMapper.modelToDto(updatedParoisseAccess);
    }

    @Override
    @Transactional(readOnly = true)
    public ParoisseAccessResponse getByPublicId(UUID publicId) {
        if (publicId == null) {
            throw new BusinessRuleException("L'identifiant de l'accès est obligatoire");
        }
        ParoisseAccess paroisseAccess = paroisseAccessRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Accès paroisse introuvable"));

        return paroisseAccessMapper.modelToDto(paroisseAccess);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParoisseAccessResponse> getAll() {
        return paroisseAccessRepository.findByStatusDelFalse()
                .stream()
                .map(paroisseAccessMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParoisseAccessResponse> getByUser(UUID userPublicId) {
        if (userPublicId == null) {
            throw new BusinessRuleException("L'utilisateur est obligatoire");
        }
        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        return paroisseAccessRepository.findByUserAndStatusDelFalse(user)
                .stream()
                .map(paroisseAccessMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParoisseAccessResponse> getByParoisse(UUID paroissePublicId) {
        if (paroissePublicId == null) {
            throw new BusinessRuleException("La paroisse est obligatoire");
        }
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return paroisseAccessRepository.findByParoisseAndStatusDelFalse(paroisse)
                .stream()
                .map(paroisseAccessMapper::modelToDto)
                .toList();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        if (publicId == null) {
            throw new BusinessRuleException("L'identifiant de l'accès est obligatoire");
        }
        ParoisseAccess paroisseAccess = paroisseAccessRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Accès paroisse introuvable"));

        if (Boolean.TRUE.equals(paroisseAccess.getUser().getIsActive())
                && Boolean.TRUE.equals(paroisseAccess.getActive())) {
            throw new BusinessRuleException(
                    "Désactivez d'abord l'utilisateur avant de supprimer son unique accès paroissial"
            );
        }

        paroisseAccess.setActive(false);
        paroisseAccess.setStatusDel(true);
        paroisseAccessRepository.save(paroisseAccess);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccessToParoisse(User user, Paroisse paroisse) {
        if (user == null || paroisse == null) {
            return false;
        }

        if (Boolean.TRUE.equals(user.getIsGlobal())) {
            return true;
        }

        return paroisseAccessRepository.existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(user, paroisse);
    }

    private void validateRequest(ParoisseAccessRequest request) {
        if (request == null) {
            throw new BusinessRuleException("La requête d'accès paroissial est obligatoire");
        }
        if (request.userPublicId() == null) {
            throw new BusinessRuleException("L'utilisateur est obligatoire");
        }
        if (request.paroissePublicId() == null) {
            throw new BusinessRuleException("La paroisse est obligatoire");
        }
        if (request.roleParoisse() == null) {
            throw new BusinessRuleException("Le rôle paroisse est obligatoire");
        }
        if (request.active() == null) {
            throw new BusinessRuleException("Le statut actif est obligatoire");
        }
    }

    private User findEligibleUser(UUID publicId) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (Boolean.TRUE.equals(user.getIsGlobal())) {
            throw new BusinessRuleException(
                    "Un utilisateur global ne doit pas être associé à une paroisse"
            );
        }
        return user;
    }

    private Paroisse findActiveParoisse(UUID publicId) {
        return paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .filter(paroisse -> Boolean.TRUE.equals(paroisse.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse active introuvable"));
    }

    private void validateAccessStateForUser(User user, Boolean requestedActive) {
        if (Boolean.TRUE.equals(user.getIsActive()) && !Boolean.TRUE.equals(requestedActive)) {
            throw new BusinessRuleException(
                    "Un utilisateur actif doit conserver un accès paroissial actif"
            );
        }
    }

    private void ensureSingleActiveParoisse(
            User user,
            ParoisseAccess currentAccess,
            Boolean requestedActive
    ) {
        if (!Boolean.TRUE.equals(requestedActive)) {
            return;
        }

        boolean hasAnotherActiveAccess = paroisseAccessRepository
                .findByUserAndActiveTrueAndStatusDelFalse(user)
                .stream()
                .anyMatch(access -> currentAccess == null
                        || !Objects.equals(access.getPublicId(), currentAccess.getPublicId()));

        if (hasAnotherActiveAccess) {
            throw new BusinessRuleException(
                    "Un utilisateur local ne peut avoir qu'une seule paroisse active"
            );
        }
    }

    private void ensureActiveUserKeepsAccess(
            ParoisseAccess currentAccess,
            User requestedUser,
            Boolean requestedActive
    ) {
        User currentUser = currentAccess.getUser();
        boolean removesCurrentActiveAccess = Boolean.TRUE.equals(currentAccess.getActive())
                && (!currentUser.getPublicId().equals(requestedUser.getPublicId())
                || !Boolean.TRUE.equals(requestedActive));

        if (removesCurrentActiveAccess && Boolean.TRUE.equals(currentUser.getIsActive())) {
            throw new BusinessRuleException(
                    "Désactivez d'abord l'utilisateur avant de retirer son unique accès paroissial"
            );
        }
    }
}
