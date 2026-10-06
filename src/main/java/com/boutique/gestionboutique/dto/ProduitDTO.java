package com.boutique.gestionboutique.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Création / modification d'un produit.
 * Le stock initial n'est saisi qu'à la création (règle P6) : il est obligatoire alors, et ignoré à la
 * modification. Ce contrôle dépend du contexte, il est donc fait dans le contrôleur.
 */
public record ProduitDTO(

        @NotBlank(message = "Le nom du produit est obligatoire.")
        @Size(max = 150, message = "150 caractères maximum.")
        String nom,

        @NotNull(message = "Le prix de vente est obligatoire.")
        @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif.")
        @Digits(integer = 10, fraction = 2, message = "Au plus 2 décimales (10 chiffres avant la virgule).")
        BigDecimal prixVente,

        @Min(value = 0, message = "Le stock ne peut pas être négatif.")
        Integer quantiteStock,

        @NotNull(message = "Le seuil d'alerte est obligatoire.")
        @Min(value = 0, message = "Le seuil ne peut pas être négatif.")
        Integer seuilAlerte,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate datePeremption,

        @NotNull(message = "La catégorie est obligatoire.")
        Integer categorieId
) {

    /** Formulaire vide : stock initial à 0 et seuil d'alerte à 5. */
    public static ProduitDTO vide() {
        return new ProduitDTO(null, null, 0, 5, null, null);
    }
}
