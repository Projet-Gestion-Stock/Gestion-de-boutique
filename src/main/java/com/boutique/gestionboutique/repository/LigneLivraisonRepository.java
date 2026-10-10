package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.LigneLivraison;
import com.boutique.gestionboutique.model.LigneLivraisonId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LigneLivraisonRepository extends JpaRepository<LigneLivraison, LigneLivraisonId> {

    /** Lots encore en stock et datés, du plus proche de la péremption au plus lointain. */
    @EntityGraph(attributePaths = "produit")
    List<LigneLivraison> findByQuantiteRestanteGreaterThanAndDatePeremptionIsNotNullOrderByDatePeremptionAsc(int quantite);

    /** Lots encore en stock qui périment au plus tard à la date donnée (produits actifs seulement). */
    @EntityGraph(attributePaths = "produit")
    List<LigneLivraison> findByQuantiteRestanteGreaterThanAndDatePeremptionLessThanEqualAndProduitActifTrueOrderByDatePeremptionAsc(
            int quantite, LocalDate limite);

    /** Lots périmés encore en stock. */
    @EntityGraph(attributePaths = "produit")
    List<LigneLivraison> findByQuantiteRestanteGreaterThanAndDatePeremptionLessThan(int quantite, LocalDate date);

    /** Lots encore en stock des produits donnés (utilisé par la vente). */
    List<LigneLivraison> findByProduitIdInAndQuantiteRestanteGreaterThan(Collection<Integer> idsProduits, int quantite);
}