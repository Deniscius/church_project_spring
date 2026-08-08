package com.eyram.dev.church_project_spring.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "paroisseAccesses", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    User toEntity(UserRequest request);

    default UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
                user.getPublicId(),
                user.getNom(),
                user.getPrenom(),
                user.getUsername(),
                user.getEmail(),
                user.getTelephone(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getIsActive(),
                user.getIsGlobal()
        );
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "paroisseAccesses", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(
            target = "role",
            expression = "java(request.role() != null ? request.role() : user.getRole())"
    )
    void updateEntityFromRequest(
            UserRequest request,
            @MappingTarget User user
    );
}
