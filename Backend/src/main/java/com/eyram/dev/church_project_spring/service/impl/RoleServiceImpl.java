package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.RoleRequest;
import com.eyram.dev.church_project_spring.DTO.response.RoleResponse;
import com.eyram.dev.church_project_spring.entities.Role;
import com.eyram.dev.church_project_spring.enums.RoleEnum;
import com.eyram.dev.church_project_spring.mappers.RoleMapper;
import com.eyram.dev.church_project_spring.repository.RoleRepository;
import com.eyram.dev.church_project_spring.service.RoleService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleServiceImpl(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public RoleResponse roleToRoleResponse(Role role) {
        return roleMapper.toResponse(role);
    }

    @Override
    public RoleResponse create(RoleRequest request) {
        Role role = roleMapper.toEntity(request);
        Role saved = roleRepository.save(role);
        return roleMapper.toResponse(saved);
    }

    @Override
    public RoleResponse getById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role non trouvé avec id : " + id));
        return roleMapper.toResponse(role);
    }

    @Override
    public RoleResponse getByPublicId(String publicId) {
        Role role = roleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Role non trouvé avec publicId : " + publicId));
        return roleMapper.toResponse(role);
    }

    @Override
    public List<RoleResponse> getAll() {
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    public RoleResponse update(String publicId, RoleRequest request) {
        Role role = roleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Role non trouvé avec publicId : " + publicId));

        role.setRole(request.role());
        role.setDescription(request.description());


        Role updated = roleRepository.save(role);
        return roleMapper.toResponse(updated);
    }

    @Override
    public void delete(String publicId) {
        Role role = roleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Role non trouvé avec publicId : " + publicId));

        roleRepository.delete(role);
    }
}
