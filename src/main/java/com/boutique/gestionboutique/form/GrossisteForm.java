package com.boutique.gestionboutique.form;

import com.boutique.gestionboutique.model.Grossiste;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Création / modification d'un grossiste
public record GrossisteForm(

        @NotBlank(message = "Le nom du grossiste est obligatoire.")
        @Size(max = 150, message = "150 caractères maximum.")
        String nom,

        @Size(max = 30, message = "30 caractères maximum.")
        String telephone,

        @Size(max = 255, message = "255 caractères maximum.")
        String adresse,

        @Email(message = "Adresse e-mail invalide.")
        @Size(max = 150, message = "150 caractères maximum.")
        String email
) {

    public static GrossisteForm vide() {
        return new GrossisteForm(null, null, null, null);
    }

    public static GrossisteForm depuis(Grossiste g) {
        return new GrossisteForm(g.getNom(), g.getTelephone(), g.getAdresse(), g.getEmail());
    }
}