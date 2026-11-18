package com.eyram.dev.church_project_spring.service.cache;

import com.eyram.dev.church_project_spring.config.CacheConfig;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Call only after the normal tenant/permission checks. Committed PostgreSQL revisions scope the
 * namespace to a parish and keep independent JVMs coherent without cache broadcasts.
 */
@Component
@RequiredArgsConstructor
public class ParishResponseCache {
  private static final Set<String> NAMES =
      Set.of(
          CacheConfig.DEMANDE_PAGES, CacheConfig.DASHBOARD_STATS, CacheConfig.DASHBOARD_PROGRAMMES);
  private final CacheManager caches;
  private final JdbcTemplate jdbc;

  private record Version(long parish, long global) {}

  private record Key(UUID parish, Version version, String parishName, String query) {}

  public <T> T get(String name, Paroisse parish, String query, Supplier<T> loader) {
    if (!NAMES.contains(name)) throw new IllegalArgumentException("Cache paroissial inconnu");
    // A write transaction must neither publish its uncommitted DTOs nor reuse a value
    // from outside its own changes. This also prevents a rolled-back revision being reused.
    if (!TransactionSynchronizationManager.isActualTransactionActive()
        || !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) return loader.get();
    var cache = caches.getCache(name);
    if (cache == null) return loader.get();
    Version version =
        jdbc.queryForObject(
            """
            SELECT coalesce(c.revision,0),g.revision
            FROM desktop_global_clock g
            LEFT JOIN desktop_sync_clock c ON c.parish_id=?
            WHERE g.id=1
            """,
            (row, n) -> new Version(row.getLong(1), row.getLong(2)),
            parish.getId());
    // An in-flight reader may finish after a commit, but can only fill its old key.
    // TTL and maximumSize reclaim old namespaces; no global clear or key scan is needed.
    // The parish was resolved before the revision read. Its label may still be an
    // older JPA snapshot during a concurrent rename; never pin that label to the
    // namespace used by a later request that has resolved the new parish name.
    return cache.get(new Key(parish.getPublicId(), version, parish.getNom(), query), loader::get);
  }
}
