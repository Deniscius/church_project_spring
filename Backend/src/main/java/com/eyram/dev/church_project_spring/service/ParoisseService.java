package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;

import java.util.List;
import java.util.UUID;

public interface ParoisseService {
    ParoisseResponse create( ParoisseRequest request);
    List<ParoisseResponse> list();
    ParoisseResponse update( ParoisseRequest request);
    ParoisseResponse deleteByName(String name);
    ParoisseResponse deleteByPublicId(UUID publicId);
    ParoisseResponse getByPublicId(UUID publicId);

}
