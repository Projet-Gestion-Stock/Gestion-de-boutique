package com.boutique.gestionboutique.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Une ligne de la livraison = un lot. Deux modes :
 * - produit existant (nouveau = false) : produitId est obligatoire ;
 * - nouveau produit (nouveau = true) : le bloc nouveauProduit est obligatoire, produitId est ignoré.
 * Dans les deux cas : quantité, prix d'achat et péremption (facultative) du lot.
 * Le contrôle du mode (produitId ou nouveauProduit) est fait dans LivraisonController, car il dépend du mode.
 */
public record LigneLivraisonDTO(

        boolean nouveau,

        Integer produitId,

        /** Jamais null (voir le constructeur) : le formulaire peut ainsi toujours lire ses champs. */
        ProduitDTO nouveauProduit,

        @NotNull(message = "La quantité est obligatoire.")
        @Min(value = 1, message = "La quantité doit être d'au moins 1.")
        @Max(value = 100000, message = "100 000 maximum par ligne.")
        Integer quantite,

        @NotNull(message = "Le prix d'achat est obligatoire.")
        @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif.")
        @Digits(integer = 10, fraction = 2, message = "Au plus 2 décimales.")
        BigDecimal prixAchat,

        // Règles LOT1 et LOT2 : facultative, et jamais dans le passé
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @FutureOrPresent(message = "Un lot déjà périmé ne peut pas être réceptionné.")
        LocalDate datePeremption
) {

        public LigneLivraisonDTO {
                if (nouveauProduit == null) {
                        nouveauProduit = ProduitDTO.vide();
                }
        }
}