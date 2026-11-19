package com.eyram.dev.church_project_spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.eyram.dev.church_project_spring.repositories.DashboardUnpaidRepository;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import com.eyram.dev.church_project_spring.service.DemandeService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
@EnabledIfEnvironmentVariable(named = "CI_POSTGRES_SMOKE", matches = "true")
class DashboardUnpaidIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired DashboardUnpaidRepository repository;
  @Autowired DemandeService service;
  @Autowired Clock clock;
  UUID parishPublicId;
  long parish, other, type, forfait;
  static final LocalDateTime START = LocalDateTime.of(2030, 6, 10, 10, 0);

  @BeforeEach
  void prepare() {
    parishPublicId = UUID.randomUUID();
    parish = parish(parishPublicId);
    other = parish(UUID.randomUUID());
    type =
        jdbc.queryForObject(
            """
            INSERT INTO type_demande(public_id,paroisse_id,type_principal,libelle)
            VALUES (?,?,'EUCHARISTIE','Fictif') RETURNING id
            """,
            Long.class,
            UUID.randomUUID(),
            parish);
    forfait =
        jdbc.queryForObject(
            """
            INSERT INTO forfait_tarif(public_id,type_demande_id,code_forfait,nom_forfait,
                nature_forfait,montant_forfait,nombre_celebration)
            VALUES (?,?,?,'Fictif','NORMALE',5000,1) RETURNING id
            """,
            Long.class,
            UUID.randomUUID(),
            type,
            UUID.randomUUID().toString());
    var principal =
        new UserDetailsImpl(
            UUID.randomUUID(), "Fictif", "fictif", null, true, 0, "unused", List.of(), true);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  @AfterEach
  void clearSecurity() {
    SecurityContextHolder.clearContext();
  }

  private long parish(UUID publicId) {
    return jdbc.queryForObject(
        """
        INSERT INTO paroisse(public_id,nom,adresse,doyenne_id,statut_tenant,is_active)
        SELECT ?,?,'Fictif',id,'ACTIVE',true FROM doyenne ORDER BY id LIMIT 1 RETURNING id
        """,
        Long.class,
        publicId,
        "Pagination " + publicId);
  }

  private UUID demand(long tenant, LocalDateTime at) {
    UUID publicId = UUID.randomUUID();
    long id =
        jdbc.queryForObject(
            """
            INSERT INTO demande(public_id,code_suivie,paroisse_id,type_demande_id,forfait_tarif_id,
                nom_fidele,prenom_fidele,tel_fidele,intention,montant,statut_demande,
                statut_paiement,statut_validation,created_at)
            VALUES (?,?,?,?,?,'Fictif','Test','+22890000000','Fictif',5000,'VALIDEE',
                'NON_PAYE','VALIDEE',CURRENT_TIMESTAMP) RETURNING id
            """,
            Long.class,
            publicId,
            publicId.toString(),
            tenant,
            type,
            forfait);
    jdbc.update(
        """
        INSERT INTO demande_date(public_id,demande_id,date_celebration,ordre,heure_personnalisee)
        VALUES (?,?,?,1,?)
        """,
        UUID.randomUUID(),
        id,
        at.toLocalDate(),
        at.toLocalTime());
    return publicId;
  }

  private void change(UUID id, String assignment) {
    jdbc.update("UPDATE demande SET " + assignment + " WHERE public_id=?", id);
  }

  @Test
  void paginatesInUrgencyOrderWithExactTotalsAndNoDuplicatesOrTenantLeaks() {
    var expected = new ArrayList<UUID>();
    // Insertion order differs from urgency order; equal timestamps use the database ID.
    for (int i = 0; i < 25; i++) expected.add(demand(parish, START.plusHours(25 - i / 2)));
    expected.sort(
        java.util.Comparator.comparing(
            id ->
                jdbc.queryForObject(
                    """
                    SELECT dd.date_celebration + dd.heure_personnalisee
                    FROM demande_date dd JOIN demande d ON d.id=dd.demande_id WHERE d.public_id=?
                    """,
                    LocalDateTime.class,
                    id)));
    demand(other, START.plusHours(1));
    change(demand(parish, START.plusHours(1)), "statut_paiement='PAYE'");
    change(demand(parish, START.plusHours(1)), "statut_demande='ANNULEE'");
    change(demand(parish, START.plusHours(1)), "statut_demande='REJETEE'");
    change(demand(parish, START.plusHours(1)), "statut_demande='TERMINEE'");
    change(demand(parish, START.plusHours(1)), "status_del=true");
    var deletedDate = demand(parish, START.plusHours(1));
    jdbc.update(
        "UPDATE demande_date SET status_del=true WHERE demande_id=(SELECT id FROM demande WHERE"
            + " public_id=?)",
        deletedDate);
    demand(parish, START.minusSeconds(1));
    demand(parish, START.plusDays(3).plusSeconds(1));
    // A second celebration in the window must neither duplicate a request nor make
    // an already-past first celebration eligible.
    var past = demand(parish, START.minusDays(1));
    for (UUID id : List.of(past, expected.get(0))) {
      jdbc.update(
          """
          INSERT INTO demande_date(public_id,demande_id,date_celebration,ordre)
          SELECT ?,id,?,2 FROM demande WHERE public_id=?
          """,
          UUID.randomUUID(),
          START.plusDays(2).toLocalDate(),
          id);
    }
    var found = new ArrayList<UUID>();
    for (int page = 0; page < 3; page++) {
      var result = repository.findUpcoming(parish, START, START.plusDays(3), page, 10);
      assertThat(result.totalElements()).isEqualTo(25);
      assertThat(result.totalPages()).isEqualTo(3);
      assertThat(result.last()).isEqualTo(page == 2);
      assertThat(result.content()).hasSize(page == 2 ? 5 : 10);
      result.content().forEach(row -> found.add(row.publicId()));
    }
    assertThat(found).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
    var beyond = repository.findUpcoming(parish, START, START.plusDays(3), Integer.MAX_VALUE, 100);
    assertThat(beyond.content()).isEmpty();
    assertThat(beyond.totalElements()).isEqualTo(25);
  }

  @Test
  void appliesAllTimeFallbacksAndInclusiveBoundariesBeforePagination() {
    var atStart = demand(parish, START);
    var atEnd = demand(parish, START.plusDays(3));
    var custom = demand(parish, START.plusHours(1));
    var dateSchedule = demand(parish, START.plusHours(2));
    var demandCustom = demand(parish, START.plusHours(3));
    var demandSchedule = demand(parish, START.plusHours(4));
    var midnight = demand(parish, START.plusDays(1).with(LocalTime.MIDNIGHT));
    long schedule =
        jdbc.queryForObject(
            """
            INSERT INTO horaire(public_id,paroisse_id,jour_semaine,heure_celebration)
            VALUES (?,?,'LUNDI','12:00') RETURNING id
            """,
            Long.class,
            UUID.randomUUID(),
            parish);
    change(custom, "horaire_id=" + schedule + ", heure_personnalisee='23:00'");
    jdbc.update(
        "UPDATE demande_date SET horaire_id=? WHERE demande_id=(SELECT id FROM demande WHERE"
            + " public_id=?)",
        schedule,
        custom);
    jdbc.update(
        "UPDATE demande_date SET heure_personnalisee=null, horaire_id=? WHERE demande_id=(SELECT id"
            + " FROM demande WHERE public_id=?)",
        schedule,
        dateSchedule);
    change(dateSchedule, "heure_personnalisee='23:00'");
    change(demandCustom, "heure_personnalisee='13:00', horaire_id=" + schedule);
    change(demandSchedule, "horaire_id=" + schedule);
    jdbc.update("UPDATE horaire SET heure_celebration='14:00' WHERE id=?", schedule);
    // Separate per-date schedule preserves its own 12:00 precedence.
    long dateScheduleId =
        jdbc.queryForObject(
            """
            INSERT INTO horaire(public_id,paroisse_id,jour_semaine,heure_celebration)
            VALUES (?,?,'MARDI','12:00') RETURNING id
            """,
            Long.class,
            UUID.randomUUID(),
            parish);
    jdbc.update(
        "UPDATE demande_date SET horaire_id=? WHERE demande_id=(SELECT id FROM demande WHERE"
            + " public_id=?)",
        dateScheduleId,
        dateSchedule);
    for (UUID id : List.of(demandCustom, demandSchedule, midnight)) {
      jdbc.update(
          "UPDATE demande_date SET heure_personnalisee=null WHERE demande_id=(SELECT id FROM"
              + " demande WHERE public_id=?)",
          id);
    }
    demand(parish, START.minusSeconds(1));
    demand(parish, START.plusDays(3).plusSeconds(1));
    var result = repository.findUpcoming(parish, START, START.plusDays(3), 0, 100);
    assertThat(result.content())
        .extracting(row -> row.publicId())
        .containsExactly(
            atStart, custom, dateSchedule, demandCustom, demandSchedule, midnight, atEnd);
    assertThat(result.content())
        .extracting(row -> row.premiereCelebration())
        .containsExactly(
            START,
            START.plusHours(1),
            START.plusHours(2),
            START.plusHours(3),
            START.plusHours(4),
            START.plusDays(1).with(LocalTime.MIDNIGHT),
            START.plusDays(3));
  }

  @Test
  void statsKeepExactCountWithOnlyFiveLegacyPreviewRowsAndServiceBoundsPageSizes() {
    LocalDateTime tomorrow = LocalDateTime.now(clock).plusDays(1).with(LocalTime.NOON);
    for (int i = 0; i < 25; i++) demand(parish, tomorrow.plusMinutes(i));
    var stats = service.getParoisseStats(parishPublicId);
    assertThat(stats.impayeesProchesCelebration()).isEqualTo(25);
    assertThat(stats.impayeesProches()).hasSize(5);
    assertThat(stats.recentes()).hasSize(5);
    var first = service.getUnpaidApproachingPage(parishPublicId, -5, 0);
    assertThat(first.page()).isZero();
    assertThat(first.size()).isEqualTo(1);
    assertThat(first.content()).hasSize(1);
    var bounded = service.getUnpaidApproachingPage(parishPublicId, 0, Integer.MAX_VALUE);
    assertThat(bounded.size()).isEqualTo(100);
    assertThat(bounded.content()).hasSize(25);
    assertThat(stats.impayeesProches())
        .extracting(row -> row.publicId())
        .containsExactlyElementsOf(
            bounded.content().subList(0, 5).stream().map(row -> row.publicId()).toList());
  }

  @Test
  void emptyOrProcessedPageRetainsAccurateMetadata() {
    var id = demand(parish, START.plusDays(1));
    change(id, "statut_paiement='PAYE'");
    var empty = repository.findUpcoming(parish, START, START.plusDays(3), 1, 10);
    assertThat(empty.content()).isEmpty();
    assertThat(empty.totalElements()).isZero();
    assertThat(empty.totalPages()).isZero();
    assertThat(empty.last()).isTrue();
  }
}
