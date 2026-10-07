package com.boutique.gestionboutique.dto;

import com.boutique.gestionboutique.model.Produit;

import java.math.BigDecimal;
import java.util.List;

/**
 * Données du tableau de bord. Pour un caissier, les chiffres sont les siens et les champs réservés
 * à l'administrateur (chiffre d'affaires du mois, classement) restent vides.
 */
/** Données du tableau de bord de l'administrateur (toute la boutique). */
public record TableauDeBordDTO(
        long nombreVentesJour,
        BigDecimal chiffreAffairesJour,
        BigDecimal chiffreAffairesMois,
        List<Produit> produitsEnAlerte,
        List<Produit> produitsPeremption,
        List<ProduitVenduDTO> produitsLesPlusVendus,
        int periodeJours,
        int joursPeremption
) {
}