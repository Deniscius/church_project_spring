package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.service.EnhancedUserService;
import com.eyram.dev.church_project_spring.service.UserService;
import com.eyram.dev.church_project_spring.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Façade de compatibilité pour les anciens endpoints /users.
 *
 * La logique métier canonique se trouve désormais dans {@link EnhancedUserService}.
 * Cette classe conserve le contrat historique sans dupliquer les règles de création,
 * d'affectation de paroisse, de mise à jour et de suppression.
 */
@Service
@RequiredArgsConstructor
@Deprecated(forRemoval = false)
public class UserServiceImpl implements UserService {

    private final EnhancedUserService enhancedUserService;

    @Override
    public UserResponse create(UserRequest request) {
        return enhancedUserService.createUser(
                request,
                SecurityUtils.getCurrentUserPublicId()
        );
    }

    @Override
    public UserResponse update(UUID publicId, UserRequest request) {
        return enhancedUserService.updateUser(
                publicId,
                request,
                SecurityUtils.getCurrentUserPublicId()
        );
    }

    @Override
    public UserResponse getByPublicId(UUID publicId) {
        return enhancedUserService.getUserByPublicId(publicId);
    }

    @Override
    public List<UserResponse> getAll() {
        return enhancedUserService.getAllActiveUsers();
    }

    @Override
    public void delete(UUID publicId) {
        enhancedUserService.deleteUser(
                publicId,
                SecurityUtils.getCurrentUserPublicId()
        );
    }
}
