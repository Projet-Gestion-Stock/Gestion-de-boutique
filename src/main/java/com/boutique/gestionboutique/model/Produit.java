package com.boutique.gestionboutique.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "produit")
@Getter
@Setter
@NoArgsConstructor
public class Produit {

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

    @Column(name = "date_peremption")
    private LocalDate datePeremption;

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

    public boolean isPerime() {
        return datePeremption != null && datePeremption.isBefore(LocalDate.now());
    }

    public boolean isPeremptionProche(int jours) {
        return datePeremption != null
                && !isPerime()
                && !datePeremption.isAfter(LocalDate.now().plusDays(jours));
    }
}
