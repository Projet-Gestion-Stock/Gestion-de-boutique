package com.boutique.gestionboutique.util;

import java.time.LocalDate;

// Reconstitue les critères du filtre pour les liens de pagination (seuls les critères renseignés sont repris).
public final class FiltresUrl {

    private FiltresUrl() {
    }

    public static String enUrl(LocalDate du, LocalDate au, String nomCritere, Integer idCritere) {
        StringBuilder sb = new StringBuilder();
        if (du != null) sb.append("&du=").append(du);
        if (au != null) sb.append("&au=").append(au);
        if (idCritere != null) sb.append('&').append(nomCritere).append('=').append(idCritere);
        return sb.toString();
    }
}