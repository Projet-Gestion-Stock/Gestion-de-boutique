package com.boutique.gestionboutique.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// Formulaire de saisie d'un achat
public record AchatForm(

        @Size(max = 100, message = "100 caractères maximum.")
        String nomClient,

        @NotEmpty(message = "Ajoutez au moins un produit à l'achat.")
        @Valid
        List<LigneAchatForm> lignes
) {

    public static AchatForm vide() {
        return new AchatForm(null, List.of(new LigneAchatForm(null, 1)));
    }
}