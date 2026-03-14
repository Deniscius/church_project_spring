package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse create(UserRequest request);

    UserResponse update(UUID publicId, UserRequest request);

    UserResponse getByPublicId(UUID publicId);

    List<UserResponse> getAll();

    void delete(UUID publicId);
}