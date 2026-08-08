package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;

import java.util.List;
import java.util.UUID;

/**
 * Contrat historique conservé pour la compatibilité des endpoints /users.
 *
 * Les nouvelles fonctionnalités doivent utiliser {@link EnhancedUserService},
 * qui constitue désormais l'unique implémentation métier de gestion des utilisateurs.
 */
@Deprecated(forRemoval = false)
public interface UserService {

    UserResponse create(UserRequest request);

    UserResponse update(UUID publicId, UserRequest request);

    UserResponse getByPublicId(UUID publicId);

    List<UserResponse> getAll();

    void delete(UUID publicId);
}
