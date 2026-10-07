package com.boutique.gestionboutique.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Une ligne du panier : un produit et une quantité. Le prix est fixé par le serveur à l'enregistrement. */
public record LigneAchatDTO(

        @NotNull(message = "Choisissez un produit.")
        Integer produitId,

        @NotNull(message = "La quantité est obligatoire.")
        @Min(value = 1, message = "La quantité doit être d'au moins 1.")
        @Max(value = 100000, message = "100 000 maximum par ligne.")
        Integer quantite
) {
}