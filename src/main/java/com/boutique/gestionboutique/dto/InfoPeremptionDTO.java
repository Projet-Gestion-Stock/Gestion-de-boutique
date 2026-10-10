package com.boutique.gestionboutique.dto;

import java.time.LocalDate;

/** Résumé des péremptions d'un produit, pour la liste des produits. */
public record InfoPeremptionDTO(
        LocalDate prochainePeremption,   // date du lot non périmé le plus proche (null s'il n'y en a pas)
        Long joursAvantProchaine,        // jours restants avant cette date (null s'il n'y en a pas)
        int quantitePerimee              // unités déjà périmées encore en stock
) {
}