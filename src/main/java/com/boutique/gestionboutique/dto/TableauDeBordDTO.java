package com.boutique.gestionboutique.dto;

import com.boutique.gestionboutique.model.Produit;

import java.math.BigDecimal;
import java.util.List;

/** Données du tableau de bord de l'administrateur (toute la boutique). */
public record TableauDeBordDTO(
        long nombreVentesJour,
        BigDecimal chiffreAffairesJour,
        BigDecimal chiffreAffairesMois,
        List<Produit> produitsEnAlerte,
        List<LotDTO> lotsPeremption,
        List<ProduitVenduDTO> produitsLesPlusVendus,
        int periodeJours,
        int joursPeremption
) {
}