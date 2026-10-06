package com.boutique.gestionboutique.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Une ligne de la livraison : un produit, une quantité reçue et le prix d'achat payé au grossiste. */
public record LigneLivraisonDTO(

        @NotNull(message = "Choisissez un produit.")
        Integer produitId,

        @NotNull(message = "La quantité est obligatoire.")
        @Min(value = 1, message = "La quantité doit être d'au moins 1.")
        @Max(value = 100000, message = "100 000 maximum par ligne.")
        Integer quantite,

        @NotNull(message = "Le prix d'achat est obligatoire.")
        @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif.")
        @Digits(integer = 10, fraction = 2, message = "Au plus 2 décimales.")
        BigDecimal prixAchat
) {
}