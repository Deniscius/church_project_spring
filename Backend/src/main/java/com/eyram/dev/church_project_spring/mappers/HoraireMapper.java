package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;
import com.eyram.dev.church_project_spring.entities.Horaire;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HoraireMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    Horaire toEntity(HoraireRequest horaireRequest);


    @Mapping(source = "publicId", target = "publicID")
    @Mapping(source = "heure", target = "horaire")
    HoraireResponse toResponse(Horaire horaire);
}
