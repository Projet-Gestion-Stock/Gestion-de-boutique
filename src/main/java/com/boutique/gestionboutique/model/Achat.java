package com.boutique.gestionboutique.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "achat")
@Getter
@Setter
@NoArgsConstructor
public class Achat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_achat")
    private Integer id;

    @Column(name = "date_achat", nullable = false)
    private LocalDateTime dateAchat = LocalDateTime.now();

    @Column(name = "nom_client", length = 100)
    private String nomClient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @OneToMany(mappedBy = "achat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneAchat> lignes = new ArrayList<>();

    public LigneAchat ajouterLigne(Produit produit, int quantite) {
        LigneAchat ligne = new LigneAchat(this, produit, quantite, produit.getPrixVente());
        lignes.add(ligne);
        return ligne;
    }

    public BigDecimal getTotal() {
        return lignes.stream()
                .map(LigneAchat::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Nombre total d'articles vendus (somme des quantités de toutes les lignes)
    public int getNombreArticles() {
        return lignes.stream().mapToInt(LigneAchat::getQuantite).sum();
    }
}
