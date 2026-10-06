package com.boutique.gestionboutique.dto;

import com.boutique.gestionboutique.model.Categorie;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Création / renommage d'une catégorie. */
public record CategorieDTO(

        @NotBlank(message = "Le nom de la catégorie est obligatoire.")
        @Size(max = 100, message = "100 caractères maximum.")
        String nom
) {

    public static CategorieDTO vide() {
        return new CategorieDTO(null);
    }

    public static CategorieDTO depuis(Categorie c) {
        return new CategorieDTO(c.getNom());
    }
}
