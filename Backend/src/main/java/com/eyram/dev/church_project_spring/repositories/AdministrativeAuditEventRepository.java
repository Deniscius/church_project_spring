package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.AdministrativeAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdministrativeAuditEventRepository
        extends JpaRepository<AdministrativeAuditEvent, Long> {

    List<AdministrativeAuditEvent> findTop200ByOrderByOccurredAtDesc();
}
