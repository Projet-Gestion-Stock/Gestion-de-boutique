package com.boutique.gestionboutique.dto;

import java.time.LocalDate;

// Résumé des péremptions d'un produit, pour la liste des produits.
public record InfoPeremptionDTO(
        LocalDate prochainePeremption,   // date du lot non périmé le plus proche
        Long joursAvantProchaine,        // jours restants avant cette date
        int quantitePerimee              // unités déjà périmées encore en stock
) {
}