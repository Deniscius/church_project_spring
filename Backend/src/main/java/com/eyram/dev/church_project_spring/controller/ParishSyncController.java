package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.service.desktop.DesktopDeltaService;
import com.eyram.dev.church_project_spring.service.desktop.DesktopRevisionService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/desktop/paroisses/{parishId}/sync")
@PreAuthorize("hasAuthority('demand:read')")
public class ParishSyncController {
  private final DesktopDeltaService deltas;
  private final DesktopRevisionService revisions;

  @GetMapping("/state")
  public Map<String, Object> state(@PathVariable UUID parishId) {
    return Map.of("cursor", deltas.cursor(parishId), "catalogue", revisions.catalogue(parishId));
  }

  @GetMapping("/changes")
  public DesktopDeltaService.Page changes(@PathVariable UUID parishId, @RequestParam long after) {
    return deltas.changes(parishId, after);
  }
}
