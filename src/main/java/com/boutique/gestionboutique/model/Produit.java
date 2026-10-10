package com.boutique.gestionboutique.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produit")
@Getter
@Setter
@NoArgsConstructor
public class Produit {

    /** Image affichée pour un produit qui n'en a pas (static/images/logo-defaut.jpg). */
    public static final String IMAGE_PAR_DEFAUT = "/images/logo-defaut.jpg";

    /** Adresse sous laquelle les images envoyées sont servies (voir WebConfig). */
    public static final String URL_IMAGES = "/uploads/produits/";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produit")
    private Integer id;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "prix_vente", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixVente;

    @Column(name = "quantite_stock", nullable = false)
    private int quantiteStock = 0;

    @Column(name = "seuil_alerte", nullable = false)
    private int seuilAlerte = 5;

    @Column(name = "image", length = 255)
    private String image;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categorie", nullable = false)
    private Categorie categorie;

    public boolean isStockFaible() {
        return quantiteStock <= seuilAlerte;
    }

    /** Adresse de l'image à afficher : celle du produit, ou l'image par défaut. */
    public String getImageUrl() {
        return (image == null || image.isBlank()) ? IMAGE_PAR_DEFAUT : URL_IMAGES + image;
    }
}