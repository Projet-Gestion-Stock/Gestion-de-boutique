package com.boutique.gestionboutique.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Période de recherche [debut, fin[ construite à partir de deux jours INCLUS (règle FI2).
 * Une borne absente est remplacée par une date très lointaine, ce qui évite de passer des paramètres
 * nuls dans les requêtes.
 */
public record Periode(LocalDateTime debut, LocalDateTime fin) {

    private static final LocalDateTime DEBUT_ILLIMITE = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime FIN_ILLIMITEE = LocalDateTime.of(3000, 1, 1, 0, 0);

    /** du = premier jour inclus, au = dernier jour inclus ; null = pas de limite. */
    public static Periode entre(LocalDate du, LocalDate au) {
        return new Periode(
                du != null ? du.atStartOfDay() : DEBUT_ILLIMITE,
                au != null ? au.plusDays(1).atStartOfDay() : FIN_ILLIMITEE);
    }
}
