package com.eyram.dev.church_project_spring.service.desktop;

import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.DemandeService;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DesktopDeltaService {
  private final JdbcTemplate jdbc;
  private final ParoisseRepository parishes;
  private final TenantAccessService access;
  private final DemandeService demands;

  public record Page(
      long cursor, boolean hasMore, List<DemandeResponse> upserts, List<UUID> deleted) {}

  @Transactional(readOnly = true)
  public long cursor(UUID parishId) {
    long id = authorizedParish(parishId);
    return current(id);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Page changes(UUID parishId, long after) {
    long parish = authorizedParish(parishId);
    long high = current(parish);
    if (after < 0 || after > high)
      throw new com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException(
          "Curseur invalide : rechargez les données de la paroisse.");
    var changes =
        jdbc.query(
            "SELECT demand_public_id,revision FROM desktop_demand_change WHERE parish_id=? AND"
                + " revision>? AND revision<=? ORDER BY revision LIMIT 101",
            (r, n) -> Map.entry(r.getObject(1, UUID.class), r.getLong(2)),
            parish,
            after,
            high);
    boolean more = changes.size() > 100;
    var page = changes.stream().limit(100).toList();
    var ids = page.stream().map(Map.Entry::getKey).toList();
    var rows = demands.getSyncChanges(parishId, ids);
    var live = new HashSet<UUID>();
    rows.forEach(row -> live.add(row.publicId()));
    return new Page(
        more ? page.get(page.size() - 1).getValue() : high,
        more,
        rows,
        ids.stream().filter(id -> !live.contains(id)).toList());
  }

  private long current(long parish) {
    return jdbc.queryForObject(
        "SELECT coalesce((SELECT revision FROM desktop_sync_clock WHERE parish_id=?),0)",
        Long.class,
        parish);
  }

  private long authorizedParish(UUID parishId) {
    var parish =
        parishes
            .findByPublicIdAndStatusDelFalse(parishId)
            .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
    access.checkParoisseAccess(parish);
    return parish.getId();
  }
}
