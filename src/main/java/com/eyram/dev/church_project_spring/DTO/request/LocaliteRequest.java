package com.eyram.dev.church_project_spring.DTO.request;

public record LocaliteRequest(
        String ville,
        String quartier,
        Boolean statusDel

) {
}
