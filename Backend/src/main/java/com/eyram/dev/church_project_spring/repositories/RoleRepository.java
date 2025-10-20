package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Role;
import com.eyram.dev.church_project_spring.enums.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findByPublicId(String publicId);
    Optional<Role> findByRole(RoleEnum role);
}
