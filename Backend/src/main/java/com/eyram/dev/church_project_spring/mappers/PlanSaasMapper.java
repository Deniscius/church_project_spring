package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.PlanSaasCreateRequest;
import com.eyram.dev.church_project_spring.DTO.request.PlanSaasRequest;
import com.eyram.dev.church_project_spring.DTO.response.PlanSaasResponse;
import com.eyram.dev.church_project_spring.entities.PlanSaas;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface PlanSaasMapper {

    PlanSaasResponse modelToDto(PlanSaas entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PlanSaas createEntityFromDto(PlanSaasCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "statusDel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(PlanSaasRequest request, @MappingTarget PlanSaas entity);
}
