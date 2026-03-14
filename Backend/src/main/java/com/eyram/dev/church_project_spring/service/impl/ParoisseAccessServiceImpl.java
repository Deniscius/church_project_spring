package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAccessRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseAccessResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.mappers.ParoisseAccessMapper;
import com.eyram.dev.church_project_spring.repository.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repository.UserRepository;
import com.eyram.dev.church_project_spring.service.ParoisseAccessService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParoisseAccessServiceImpl implements ParoisseAccessService {

    private final ParoisseAccessRepository paroisseAccessRepository;
    private final UserRepository userRepository;
    private final ParoisseRepository paroisseRepository;
    private final ParoisseAccessMapper paroisseAccessMapper;

    @Override
    public ParoisseAccessResponse create(ParoisseAccessRequest request) {

        User user = userRepository.findByPublicIdAndStatusDelFalse(request.userPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        if (paroisseAccessRepository.existsByUserAndParoisseAndStatusDelFalse(user, paroisse)) {
            throw new AlreadyExistException("Cet accès existe déjà pour cet utilisateur et cette paroisse");
        }

        ParoisseAccess paroisseAccess = paroisseAccessMapper.toEntity(request);
        paroisseAccess.setUser(user);
        paroisseAccess.setParoisse(paroisse);

        ParoisseAccess savedParoisseAccess = paroisseAccessRepository.save(paroisseAccess);
        return paroisseAccessMapper.toResponse(savedParoisseAccess);
    }

    @Override
    public ParoisseAccessResponse update(UUID publicId, ParoisseAccessRequest request) {

        ParoisseAccess existingParoisseAccess = paroisseAccessRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Accès paroisse introuvable"));

        User user = userRepository.findByPublicIdAndStatusDelFalse(request.userPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        boolean accessChanged =
                !existingParoisseAccess.getUser().getPublicId().equals(request.userPublicId()) ||
                        !existingParoisseAccess.getParoisse().getPublicId().equals(request.paroissePublicId());

        if (accessChanged && paroisseAccessRepository.existsByUserAndParoisseAndStatusDelFalse(user, paroisse)) {
            throw new AlreadyExistException("Cet accès existe déjà pour cet utilisateur et cette paroisse");
        }

        paroisseAccessMapper.updateEntityFromRequest(request, existingParoisseAccess);
        existingParoisseAccess.setUser(user);
        existingParoisseAccess.setParoisse(paroisse);

        ParoisseAccess updatedParoisseAccess = paroisseAccessRepository.save(existingParoisseAccess);
        return paroisseAccessMapper.toResponse(updatedParoisseAccess);
    }

    @Override
    public ParoisseAccessResponse getByPublicId(UUID publicId) {
        ParoisseAccess paroisseAccess = paroisseAccessRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Accès paroisse introuvable"));

        return paroisseAccessMapper.toResponse(paroisseAccess);
    }

    @Override
    public List<ParoisseAccessResponse> getAll() {
        return paroisseAccessRepository.findByStatusDelFalse()
                .stream()
                .map(paroisseAccessMapper::toResponse)
                .toList();
    }

    @Override
    public List<ParoisseAccessResponse> getByUser(UUID userPublicId) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        return paroisseAccessRepository.findByUserAndStatusDelFalse(user)
                .stream()
                .map(paroisseAccessMapper::toResponse)
                .toList();
    }

    @Override
    public List<ParoisseAccessResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return paroisseAccessRepository.findByParoisseAndStatusDelFalse(paroisse)
                .stream()
                .map(paroisseAccessMapper::toResponse)
                .toList();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        ParoisseAccess paroisseAccess = paroisseAccessRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Accès paroisse introuvable"));

        paroisseAccess.setStatusDel(true);
        paroisseAccessRepository.save(paroisseAccess);
    }

    @Override
    public boolean hasAccessToParoisse(User user, Paroisse paroisse) {
        if (Boolean.TRUE.equals(user.getIsGlobal())) {
            return true;
        }

        return paroisseAccessRepository.existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(user, paroisse);
    }
}