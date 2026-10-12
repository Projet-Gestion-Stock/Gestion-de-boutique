package com.boutique.gestionboutique.form;

import com.boutique.gestionboutique.model.Utilisateur;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ProfilForm(

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "100 caractères maximum.")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100, message = "100 caractères maximum.")
        String prenom,

        @NotBlank(message = "Le sexe est obligatoire.")
        @Pattern(regexp = "[MF]", message = "Sexe invalide.")
        String sexe,

        @NotNull(message = "La date de naissance est obligatoire.")
        @Past(message = "La date de naissance doit être dans le passé.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateNaissance
) {

    public static ProfilForm depuis(Utilisateur u) {
        return new ProfilForm(u.getNom(), u.getPrenom(), String.valueOf(u.getSexe()), u.getDateNaissance());
    }
}
