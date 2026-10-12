package com.boutique.gestionboutique.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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

    // Date de péremption du lot
    @Column(name = "date_peremption")
    private LocalDate datePeremption;

    // Unités du lot encore en stock
    @Column(name = "quantite_restante", nullable = false)
    private int quantiteRestante;

    // Unités retirées du stock parce qu'elles étaient périmées
    @Column(name = "quantite_retiree", nullable = false)
    private int quantiteRetiree;

    public LigneLivraison(Livraison livraison, Produit produit, int quantite,
                          BigDecimal prixAchat, LocalDate datePeremption) {
        this.livraison = livraison;
        this.produit = produit;
        this.quantite = quantite;
        this.prixAchat = prixAchat;
        this.datePeremption = datePeremption;
        this.quantiteRestante = quantite;
        this.quantiteRetiree = 0;
    }

    public BigDecimal getSousTotal() {
        return prixAchat.multiply(BigDecimal.valueOf(quantite));
    }

    // Vrai si la date de péremption du lot est dépassée.
    public boolean isPerime() {
        return datePeremption != null && datePeremption.isBefore(LocalDate.now());
    }

    public int getQuantiteVendue() {
        return quantite - quantiteRestante - quantiteRetiree;
    }

    public Long getJoursAvantPeremption() {
        return datePeremption == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), datePeremption);
    }
}