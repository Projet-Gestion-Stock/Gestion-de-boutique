package com.boutique.gestionboutique.util;

import java.time.LocalDate;

/** Reconstitue les critères du filtre pour les liens de pagination (seuls les critères renseignés sont repris). */
public final class Filtres {

    private Filtres() {
    }

    /** Retourne par exemple "&du=2026-10-01&caissierId=3" (chaîne vide si aucun critère). */
    public static String enUrl(LocalDate du, LocalDate au, String nomCritere, Integer idCritere) {
        StringBuilder sb = new StringBuilder();
        if (du != null) sb.append("&du=").append(du);
        if (au != null) sb.append("&au=").append(au);
        if (idCritere != null) sb.append('&').append(nomCritere).append('=').append(idCritere);
        return sb.toString();
    }
}