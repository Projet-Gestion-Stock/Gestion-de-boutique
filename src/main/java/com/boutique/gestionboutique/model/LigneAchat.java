package com.boutique.gestionboutique.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "contient_achat")
@Getter
@Setter
@NoArgsConstructor
public class LigneAchat {

    @EmbeddedId
    private LigneAchatId id = new LigneAchatId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idAchat")
    @JoinColumn(name = "id_achat")
    private Achat achat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idProduit")
    @JoinColumn(name = "id_produit")
    private Produit produit;

    @Column(name = "quantite", nullable = false)
    private int quantite;

    @Column(name = "prix_unitaire", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    public LigneAchat(Achat achat, Produit produit, int quantite, BigDecimal prixUnitaire) {
        this.achat = achat;
        this.produit = produit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getSousTotal() {
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }
}
