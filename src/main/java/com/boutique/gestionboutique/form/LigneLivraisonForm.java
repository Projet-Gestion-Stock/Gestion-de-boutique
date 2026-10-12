package com.boutique.gestionboutique.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LigneLivraisonForm(

        boolean nouveau,

        Integer produitId,

        ProduitForm nouveauProduit,

        @NotNull(message = "La quantité est obligatoire.")
        @Min(value = 1, message = "La quantité doit être d'au moins 1.")
        @Max(value = 100000, message = "100 000 maximum par ligne.")
        Integer quantite,

        @NotNull(message = "Le prix d'achat est obligatoire.")
        @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif.")
        @Digits(integer = 10, fraction = 2, message = "Au plus 2 décimales.")
        BigDecimal prixAchat,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @FutureOrPresent(message = "Un lot déjà périmé ne peut pas être réceptionné.")
        LocalDate datePeremption
) {

        public LigneLivraisonForm {
                if (nouveauProduit == null) {
                        nouveauProduit = ProduitForm.vide();
                }
        }
}