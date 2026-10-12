package com.boutique.gestionboutique.dto;

import com.boutique.gestionboutique.model.Produit;

import java.math.BigDecimal;

// Ligne du classement des produits les plus vendus.
public record ProduitVenduDTO(String nom, String image, Long quantite, BigDecimal montant) {

    public String imageUrl() {
        return (image == null || image.isBlank()) ? Produit.IMAGE_PAR_DEFAUT : Produit.URL_IMAGES + image;
    }
}