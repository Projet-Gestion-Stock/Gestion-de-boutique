package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.LigneLivraison;
import com.boutique.gestionboutique.model.LigneLivraisonId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /** Coût d'achat total des livraisons de la période [debut, fin[ ; null s'il n'y en a aucune. */
    @Query("select sum(l.quantite * l.prixAchat) from LigneLivraison l "
            + "where l.livraison.dateLivraison >= :debut and l.livraison.dateLivraison < :fin")
    BigDecimal coutEntre(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /** Même chose, pour un grossiste. */
    @Query("select sum(l.quantite * l.prixAchat) from LigneLivraison l "
            + "where l.livraison.dateLivraison >= :debut and l.livraison.dateLivraison < :fin "
            + "and l.livraison.grossiste.id = :idGrossiste")
    BigDecimal coutEntreParGrossiste(@Param("debut") LocalDateTime debut,
                                     @Param("fin") LocalDateTime fin,
                                     @Param("idGrossiste") Integer idGrossiste);
}