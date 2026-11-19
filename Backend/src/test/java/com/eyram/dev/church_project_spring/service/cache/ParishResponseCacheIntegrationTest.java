package com.eyram.dev.church_project_spring.service.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.eyram.dev.church_project_spring.config.CacheConfig;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.admin-seed.enabled=false",
      "app.tenant-catalog.backfill-on-startup=false",
      "app.demande.unpaid-auto-cancel-enabled=false",
      "app.demande.unpaid-reminder-enabled=false",
      "app.demande.celebration-reminder-enabled=false",
      "app.demande.celebration-auto-complete-enabled=false",
      "fedapay.enabled=false",
      "saspay.poll-delay-ms=3600000",
      "spring.flyway.repair-on-migrate=false"
    })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfEnvironmentVariable(named = "CI_POSTGRES_SMOKE", matches = "true")
class ParishResponseCacheIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactions;
  @Autowired ParishResponseCache cache;
  Paroisse a, b;
  private static final List<String> CACHES =
      List.of(
          CacheConfig.DEMANDE_PAGES, CacheConfig.DASHBOARD_STATS, CacheConfig.DASHBOARD_PROGRAMMES);

  @BeforeEach
  void prepare() {
    a = parish("Cache A");
    b = parish("Cache B");
  }

  @AfterEach
  void removeFixtures() {
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              for (Paroisse parish : new Paroisse[] {a, b}) {
                if (parish == null) continue;
                jdbc.update(
                    "DELETE FROM parish_storage_placement WHERE parish_id=?", parish.getPublicId());
                jdbc.update("DELETE FROM paroisse WHERE id=?", parish.getId());
              }
            });
  }

  private Paroisse parish(String name) {
    var parish = new Paroisse();
    parish.setPublicId(UUID.randomUUID());
    parish.setNom(name);
    parish.setId(
        jdbc.queryForObject(
            """
            INSERT INTO paroisse(public_id,nom,adresse,doyenne_id,statut_tenant,is_active)
            SELECT ?,?,'Fictif',id,'ACTIVE',true FROM doyenne ORDER BY id LIMIT 1 RETURNING id
            """,
            Long.class,
            parish.getPublicId(),
            name));
    return parish;
  }

  private String name(Paroisse parish) {
    return jdbc.queryForObject("SELECT nom FROM paroisse WHERE id=?", String.class, parish.getId());
  }

  private void rename(Paroisse parish, String name) {
    jdbc.update(
        "UPDATE paroisse SET nom=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", name, parish.getId());
  }

  private <T> T read(Supplier<T> supplier) {
    var tx = new TransactionTemplate(transactions);
    tx.setReadOnly(true);
    return tx.execute(status -> supplier.get());
  }

  private String read(
      ParishResponseCache node, String type, Paroisse parish, Supplier<String> loader) {
    return read(() -> node.get(type, parish, "fixture", loader));
  }

  @Test
  void committedChangeReloadsOnlyTheAffectedParishForEveryResponseCache() {
    for (String type : CACHES) {
      var loadsA = new AtomicInteger();
      var loadsB = new AtomicInteger();
      Supplier<String> loadA =
          () -> {
            loadsA.incrementAndGet();
            return name(a);
          };
      Supplier<String> loadB =
          () -> {
            loadsB.incrementAndGet();
            return name(b);
          };
      assertThat(read(cache, type, a, loadA)).isEqualTo(name(a));
      assertThat(read(cache, type, b, loadB)).isEqualTo("Cache B");
      assertThat(read(cache, type, a, loadA)).isEqualTo(name(a));
      new TransactionTemplate(transactions)
          .executeWithoutResult(status -> rename(a, "Changed " + type));
      assertThat(read(cache, type, a, loadA)).isEqualTo("Changed " + type);
      assertThat(read(cache, type, b, loadB)).isEqualTo("Cache B");
      assertThat(loadsA.get()).isEqualTo(2);
      assertThat(loadsB.get()).isEqualTo(1);
    }
  }

  @Test
  void rollbackKeepsThePreviousEntryAndCannotPublishUncommittedData() {
    for (String type : CACHES) {
      String original = name(a);
      assertThat(read(cache, type, a, () -> name(a))).isEqualTo(original);
      new TransactionTemplate(transactions)
          .executeWithoutResult(
              status -> {
                rename(a, "Uncommitted");
                assertThat(cache.get(type, a, "fixture", () -> name(a))).isEqualTo("Uncommitted");
                status.setRollbackOnly();
              });
      assertThat(
              read(
                  cache,
                  type,
                  a,
                  () -> {
                    throw new AssertionError("Rollback invalidated the cache");
                  }))
          .isEqualTo(original);
      // This commit reuses the revision rolled back above; no dirty entry may exist for it.
      new TransactionTemplate(transactions)
          .executeWithoutResult(status -> rename(a, "Committed " + type));
      assertThat(read(cache, type, a, () -> name(a))).isEqualTo("Committed " + type);
    }
  }

  @Test
  void independentApplicationCachesObserveTheSameCommittedVersion() {
    var manager = (SimpleCacheManager) new CacheConfig().cacheManager();
    manager.afterPropertiesSet();
    var otherNode = new ParishResponseCache(manager, jdbc);
    for (String type : CACHES) {
      read(cache, type, a, () -> name(a));
      read(otherNode, type, a, () -> name(a));
      new TransactionTemplate(transactions)
          .executeWithoutResult(status -> rename(a, "Shared " + type));
      assertThat(read(cache, type, a, () -> name(a))).isEqualTo("Shared " + type);
      assertThat(read(otherNode, type, a, () -> name(a))).isEqualTo("Shared " + type);
    }
  }

  @Test
  void aConcurrentParishRenameCannotPinTheOldLabelToTheNewRevision() {
    new TransactionTemplate(transactions).executeWithoutResult(status -> rename(a, "New label"));
    // A request that resolved its parish before the commit still holds its old label.
    assertThat(read(cache, CacheConfig.DASHBOARD_STATS, a, a::getNom)).isEqualTo("Cache A");
    var fresh = new Paroisse();
    fresh.setId(a.getId());
    fresh.setPublicId(a.getPublicId());
    fresh.setNom(name(a));
    assertThat(read(cache, CacheConfig.DASHBOARD_STATS, fresh, fresh::getNom)).isEqualTo("New label");
  }

  @Test
  void oldInFlightLoaderCannotOverwriteTheNewCommittedNamespace() throws Exception {
    var captured = new CountDownLatch(1);
    var finish = new CountDownLatch(1);
    var pool = Executors.newSingleThreadExecutor();
    try {
      var oldRead =
          pool.submit(
              () ->
                  read(
                      cache,
                      CacheConfig.DEMANDE_PAGES,
                      a,
                      () -> {
                        String old = name(a);
                        captured.countDown();
                        try {
                          if (!finish.await(5, TimeUnit.SECONDS))
                            throw new AssertionError("Test timeout");
                        } catch (InterruptedException e) {
                          throw new IllegalStateException(e);
                        }
                        return old;
                      }));
      assertThat(captured.await(5, TimeUnit.SECONDS)).isTrue();
      new TransactionTemplate(transactions)
          .executeWithoutResult(status -> rename(a, "New committed value"));
      finish.countDown();
      assertThat(oldRead.get(5, TimeUnit.SECONDS)).isEqualTo("Cache A");
      assertThat(read(cache, CacheConfig.DEMANDE_PAGES, a, () -> name(a)))
          .isEqualTo("New committed value");
      assertThat(
              read(
                  cache,
                  CacheConfig.DEMANDE_PAGES,
                  a,
                  () -> {
                    throw new AssertionError("Fresh entry was lost");
                  }))
          .isEqualTo("New committed value");
    } finally {
      finish.countDown();
      pool.shutdownNow();
    }
  }
}
