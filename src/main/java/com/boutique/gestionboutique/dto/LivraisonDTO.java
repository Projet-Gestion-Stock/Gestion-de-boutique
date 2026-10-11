package com.boutique.gestionboutique.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Saisie d'une livraison : un grossiste et au moins une ligne de produit. */
public record LivraisonDTO(

        @NotNull(message = "Le grossiste est obligatoire.")
        Integer grossisteId,

        @NotEmpty(message = "Ajoutez au moins un produit à la livraison.")
        @Valid
        List<LigneLivraisonDTO> lignes
) {

    /** Formulaire vide avec une première ligne (quantité 1). */
    public static LivraisonDTO vide() {
        return new LivraisonDTO(null, List.of(new LigneLivraisonDTO(false, null, null, 1, null, null)));
    }
}