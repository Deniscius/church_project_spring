package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.mappers.UserMapper;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.UserService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse create(UserRequest request) {

        if (userRepository.existsByUsernameAndStatusDelFalse(request.username())) {
            throw new AlreadyExistException("Un utilisateur avec ce nom d'utilisateur existe déjà");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
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
        user.setPassword(passwordEncoder.encode(request.password()));

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public UserResponse getByPublicId(UUID publicId) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .filter(user -> Boolean.FALSE.equals(user.getStatusDel()))
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(UUID publicId) {
        User user = userRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setStatusDel(true);
        userRepository.save(user);
    }
}