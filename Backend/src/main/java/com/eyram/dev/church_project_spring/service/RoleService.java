package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.RoleRequest;
import com.eyram.dev.church_project_spring.DTO.response.RoleResponse;

import java.util.List;

public interface RoleService {

    RoleResponse roleToRoleResponse(com.eyram.dev.church_project_spring.entities.Role role);
    RoleResponse create(RoleRequest request);
    RoleResponse getById(Long id);
    RoleResponse getByPublicId(String publicId);
    List<RoleResponse> getAll();
    RoleResponse update(String publicId, RoleRequest request);
    void delete(String publicId);
}
