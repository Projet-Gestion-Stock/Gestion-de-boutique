package com.boutique.gestionboutique.dto;

import java.time.LocalDate;

/** Un lot encore en stock et daté (alertes de péremption, page de gestion des péremptions). */
public record LotDTO(
        Integer idLivraison,
        Integer idProduit,
        String produitNom,
        String imageUrl,            // image du produit (ou image par défaut)
        LocalDate datePeremption,
        int quantiteRestante,
        long joursRestants          // négatif si le lot est déjà périmé
) {
}
