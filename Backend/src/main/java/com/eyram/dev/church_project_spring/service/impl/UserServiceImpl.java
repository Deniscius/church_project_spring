package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.mappers.UserMapper;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.UserService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ParoisseRepository paroisseRepository;
    private final ParoisseAccessRepository paroisseAccessRepository;

    @Override
    public UserResponse create(UserRequest request) {

        if (userRepository.existsByUsernameAndStatusDelFalse(request.username())) {
            throw new AlreadyExistException(
                    "Un utilisateur avec ce nom d'utilisateur existe déjà"
            );
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatusDel(false);

        User savedUser = userRepository.save(user);

        if (!Boolean.TRUE.equals(request.isGlobal())) {

            if (request.paroisses() == null || request.paroisses().isEmpty()) {
                throw new IllegalArgumentException(
                        "Une paroisse est obligatoire pour un utilisateur non global"
                );
            }

            for (ParoisseAssignmentRequest assignment : request.paroisses()) {

                Paroisse paroisse = paroisseRepository
                        .findByPublicIdAndStatusDelFalse(assignment.getParoisseId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Paroisse introuvable")
                        );

                ParoisseAccess access = new ParoisseAccess();
                access.setUser(savedUser);
                access.setParoisse(paroisse);
                access.setRoleParoisse(
                        RoleParoisse.valueOf(
                                assignment.getRoleParoisse()
                                        .trim()
                                        .toUpperCase()
                        )
                );
                access.setActive(true);
                access.setStatusDel(false);

                paroisseAccessRepository.save(access);
            }
        }

        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse update(UUID publicId, UserRequest request) {

        User user = userRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (!user.getUsername().equals(request.username())
                && userRepository.existsByUsernameAndStatusDelFalse(request.username())) {
            throw new AlreadyExistException("Un utilisateur avec ce nom d'utilisateur existe déjà");
        }

        userMapper.updateEntityFromRequest(request, user);

        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (StringUtils.hasText(request.password())) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByPublicId(UUID publicId) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findByStatusDelFalse()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(UUID publicId) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setIsActive(false);
        user.setStatusDel(true);
        userRepository.save(user);
    }
}