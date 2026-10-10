package com.boutique.gestionboutique.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Création / modification d'un produit.
 * Ni le stock ni la date de péremption ne sont saisis ici : le stock vient des livraisons
 * (règle P6) et la date de péremption est portée par chaque lot de livraison (règle LOT1).
 */
public record ProduitDTO(

        @NotBlank(message = "Le nom du produit est obligatoire.")
        @Size(max = 150, message = "150 caractères maximum.")
        String nom,

        @NotNull(message = "Le prix de vente est obligatoire.")
        @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif.")
        @Digits(integer = 10, fraction = 2, message = "Au plus 2 décimales (10 chiffres avant la virgule).")
        BigDecimal prixVente,

        @NotNull(message = "Le seuil d'alerte est obligatoire.")
        @Min(value = 0, message = "Le seuil ne peut pas être négatif.")
        Integer seuilAlerte,

        @NotNull(message = "La catégorie est obligatoire.")
        Integer categorieId
) {

    /** Formulaire vide : seuil d'alerte à 5. */
    public static ProduitDTO vide() {
        return new ProduitDTO(null, null, 5, null);
    }
}