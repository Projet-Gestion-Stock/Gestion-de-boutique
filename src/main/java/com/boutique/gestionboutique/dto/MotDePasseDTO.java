package com.boutique.gestionboutique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Changement de mot de passe. Le mot de passe actuel est exigé sauf à la première connexion */
public record MotDePasseDTO(

        String motDePasseActuel,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @Size(min = 6, max = 72, message = "Le mot de passe doit contenir entre 6 et 72 caractères.")
        String nouveauMotDePasse,

        String confirmation
) {

    public static MotDePasseDTO vide() {
        return new MotDePasseDTO(null, null, null);
    }
}
