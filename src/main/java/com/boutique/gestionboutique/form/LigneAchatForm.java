package com.boutique.gestionboutique.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LigneAchatForm(

        @NotNull(message = "Choisissez un produit.")
        Integer produitId,

        @NotNull(message = "La quantité est obligatoire.")
        @Min(value = 1, message = "La quantité doit être d'au moins 1.")
        @Max(value = 100000, message = "100 000 maximum par ligne.")
        Integer quantite
) {
}