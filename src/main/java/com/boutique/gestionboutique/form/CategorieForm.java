package com.boutique.gestionboutique.form;

import com.boutique.gestionboutique.model.Categorie;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Création / renommage d'une catégorie.
public record CategorieForm(

        @NotBlank(message = "Le nom de la catégorie est obligatoire.")
        @Size(max = 100, message = "100 caractères maximum.")
        String nom
) {

    public static CategorieForm vide() {
        return new CategorieForm(null);
    }

    public static CategorieForm depuis(Categorie c) {
        return new CategorieForm(c.getNom());
    }
}
