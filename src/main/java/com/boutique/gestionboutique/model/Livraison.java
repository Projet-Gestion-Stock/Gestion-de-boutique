package com.boutique.gestionboutique.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "livraison")
@Getter
@Setter
@NoArgsConstructor
public class Livraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_livraison")
    private Integer id;

    @Column(name = "date_livraison", nullable = false)
    private LocalDateTime dateLivraison = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_grossiste", nullable = false)
    private Grossiste grossiste;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @OneToMany(mappedBy = "livraison", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneLivraison> lignes = new ArrayList<>();

    public LigneLivraison ajouterLigne(Produit produit, int quantite, BigDecimal prixAchat, LocalDate datePeremption) {
        LigneLivraison ligne = new LigneLivraison(this, produit, quantite, prixAchat, datePeremption);
        lignes.add(ligne);
        return ligne;
    }

    public BigDecimal getTotal() {
        return lignes.stream()
                .map(LigneLivraison::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
