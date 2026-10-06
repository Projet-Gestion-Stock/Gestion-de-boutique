package com.boutique.gestionboutique.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "contient_livraison")
@Getter
@Setter
@NoArgsConstructor
public class LigneLivraison {

    @EmbeddedId
    private LigneLivraisonId id = new LigneLivraisonId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idLivraison")
    @JoinColumn(name = "id_livraison")
    private Livraison livraison;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idProduit")
    @JoinColumn(name = "id_produit")
    private Produit produit;

    @Column(name = "quantite", nullable = false)
    private int quantite;

    @Column(name = "prix_achat", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixAchat;

    public LigneLivraison(Livraison livraison, Produit produit, int quantite, BigDecimal prixAchat) {
        this.livraison = livraison;
        this.produit = produit;
        this.quantite = quantite;
        this.prixAchat = prixAchat;
    }

    public BigDecimal getSousTotal() {
        return prixAchat.multiply(BigDecimal.valueOf(quantite));
    }
}


