package com.boutique.gestionboutique.dto;

import java.math.BigDecimal;

/** Ligne du classement des produits les plus vendus. */
public record ProduitVenduDTO(String nom, Long quantite, BigDecimal montant) {
}
