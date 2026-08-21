package com.eyram.dev.church_project_spring.repositories.projection;

import java.math.BigDecimal;

/**
 * Agrégats du tableau de bord paroissial calculés en une seule requête.
 */
public interface DemandeParoisseStatsProjection {

    Long getTotal();

    Long getEnAttente();

    Long getValidees();

    BigDecimal getVolumeMontant();
}
