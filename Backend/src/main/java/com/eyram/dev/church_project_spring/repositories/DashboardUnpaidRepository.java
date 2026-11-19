package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.DTO.response.PageResponse;
import com.eyram.dev.church_project_spring.DTO.response.UnpaidApproachingResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL performs the time filtering, count and pagination before rows reach the JVM. */
@Repository
@RequiredArgsConstructor
public class DashboardUnpaidRepository {
  private final NamedParameterJdbcTemplate jdbc;

  // Native SQL deliberately includes the tenant and soft-delete predicates: Hibernate
  // filters do not apply to JDBC. Keep the count and content on exactly the same predicate.
  private static final String CELEBRATION =
      """
      (dd.date_celebration + COALESCE(dd.heure_personnalisee, dh.heure_celebration,
          d.heure_personnalisee, h.heure_celebration, TIME '00:00:00'))
      """;
  private static final String FROM =
      """
      FROM demande d
      JOIN demande_date dd ON dd.demande_id = d.id
      JOIN type_demande t ON t.id = d.type_demande_id
      LEFT JOIN horaire dh ON dh.id = dd.horaire_id
      LEFT JOIN horaire h ON h.id = d.horaire_id
      WHERE d.paroisse_id = :parishId AND d.status_del = false
        AND dd.status_del = false AND dd.ordre = 1
        AND d.statut_paiement <> 'PAYE'
        AND d.statut_demande IN ('EN_ATTENTE', 'VALIDEE')
        AND dd.date_celebration BETWEEN :startDate AND :endDate
        AND """
          + CELEBRATION
          + " BETWEEN :start AND :end ";

  public PageResponse<UnpaidApproachingResponse> findUpcoming(
      long parishId, LocalDateTime start, LocalDateTime end, int page, int size) {
    var params =
        new MapSqlParameterSource()
            .addValue("parishId", parishId)
            .addValue("startDate", start.toLocalDate())
            .addValue("endDate", end.toLocalDate())
            .addValue("start", start)
            .addValue("end", end)
            .addValue("limit", size)
            .addValue("offset", (long) page * size);
    long total = jdbc.queryForObject("SELECT count(*) " + FROM, params, Long.class);
    var rows =
        jdbc.query(
            """
            SELECT d.public_id, d.code_suivie, d.nom_fidele, d.prenom_fidele,
                   t.libelle, """
                + CELEBRATION
                + " AS celebration "
                + FROM
                + " ORDER BY celebration ASC, d.id ASC LIMIT :limit OFFSET :offset",
            params,
            (row, n) ->
                new UnpaidApproachingResponse(
                    row.getObject("public_id", UUID.class), row.getString("code_suivie"),
                    row.getString("nom_fidele"), row.getString("prenom_fidele"),
                    row.getString("libelle"), row.getTimestamp("celebration").toLocalDateTime()));
    return PageResponse.of(rows, page, size, total);
  }
}
