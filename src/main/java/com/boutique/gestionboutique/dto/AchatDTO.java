package com.boutique.gestionboutique.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Saisie d'un achat : un nom de client facultatif et au moins une ligne. */
public record AchatDTO(

        @Size(max = 100, message = "100 caractères maximum.")
        String nomClient,

        @NotEmpty(message = "Ajoutez au moins un produit à l'achat.")
        @Valid
        List<LigneAchatDTO> lignes
) {

    /** Formulaire vide avec une première ligne (quantité 1). */
    public static AchatDTO vide() {
        return new AchatDTO(null, List.of(new LigneAchatDTO(null, 1)));
    }
}