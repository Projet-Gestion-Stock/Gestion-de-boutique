package com.boutique.gestionboutique.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LivraisonForm(

        @NotNull(message = "Le grossiste est obligatoire.")
        Integer grossisteId,

        @NotEmpty(message = "Ajoutez au moins un produit à la livraison.")
        @Valid
        List<LigneLivraisonForm> lignes
) {

    public static LivraisonForm vide() {
        return new LivraisonForm(null, List.of(new LigneLivraisonForm(false, null, null, 1, null, null)));
    }
}