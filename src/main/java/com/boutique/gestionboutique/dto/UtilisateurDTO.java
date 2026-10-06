package com.boutique.gestionboutique.dto;

import com.boutique.gestionboutique.Enum.Role;
import com.boutique.gestionboutique.model.Utilisateur;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record UtilisateurDTO(

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "100 caractères maximum.")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100, message = "100 caractères maximum.")
        String prenom,

        @NotBlank(message = "Le matricule est obligatoire.")
        @Size(max = 30, message = "30 caractères maximum.")
        String matricule,

        @NotBlank(message = "Le sexe est obligatoire.")
        @Pattern(regexp = "[MF]", message = "Sexe invalide.")
        String sexe,

        @NotNull(message = "La date de naissance est obligatoire.")
        @Past(message = "La date de naissance doit être dans le passé.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateNaissance,

        @NotBlank(message = "L'identifiant est obligatoire.")
        @Size(min = 3, max = 50, message = "Entre 3 et 50 caractères.")
        @Pattern(regexp = "[A-Za-z0-9._-]*",
                message = "Lettres, chiffres, point, tiret et underscore uniquement.")
        String identifiant,

        @NotBlank(message = "Le mot de passe provisoire est obligatoire.")
        @Size(min = 6, max = 72, message = "Le mot de passe doit contenir entre 6 et 72 caractères.")
        String motDePasse,

        String confirmationMotDePasse,

        @NotNull(message = "Le rôle est obligatoire.")
        Role role
) {

    /** Formulaire vide (rôle CAISSIER proposé par défaut). */
    public static UtilisateurDTO vide() {
        return new UtilisateurDTO(null, null, null, null, null, null, null, null, Role.CAISSIER);
    }
}
