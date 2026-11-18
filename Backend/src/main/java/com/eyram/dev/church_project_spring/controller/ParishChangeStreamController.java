package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.service.desktop.ParishChangeStreams;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class ParishChangeStreamController {
  private final ParishChangeStreams streams;

  @GetMapping(
      value = "/desktop/paroisses/{parishId}/events",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("hasAuthority('demand:read')")
  public ResponseEntity<SseEmitter> events(
      @PathVariable UUID parishId, Authentication authentication) throws java.io.IOException {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("X-Accel-Buffering", "no")
        .body(streams.open(parishId, authentication));
  }
}
