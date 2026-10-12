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

    @EntityGraph(attributePaths = "produit")
    List<LigneLivraison> findByQuantiteRestanteGreaterThanAndDatePeremptionIsNotNullOrderByDatePeremptionAsc(int quantite);

    @EntityGraph(attributePaths = "produit")
    List<LigneLivraison> findByQuantiteRestanteGreaterThanAndDatePeremptionLessThanEqualAndProduitActifTrueOrderByDatePeremptionAsc(
            int quantite, LocalDate limite);

    @EntityGraph(attributePaths = "produit")
    List<LigneLivraison> findByQuantiteRestanteGreaterThanAndDatePeremptionLessThan(int quantite, LocalDate date);

    List<LigneLivraison> findByProduitIdInAndQuantiteRestanteGreaterThan(Collection<Integer> idsProduits, int quantite);

    // Coût d'achat total des livraisons pour une période
    @Query("select sum(l.quantite * l.prixAchat) from LigneLivraison l "
            + "where l.livraison.dateLivraison >= :debut and l.livraison.dateLivraison < :fin")
    BigDecimal coutEntre(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("select sum(l.quantite * l.prixAchat) from LigneLivraison l "
            + "where l.livraison.dateLivraison >= :debut and l.livraison.dateLivraison < :fin "
            + "and l.livraison.grossiste.id = :idGrossiste")
    BigDecimal coutEntreParGrossiste(@Param("debut") LocalDateTime debut,
                                     @Param("fin") LocalDateTime fin,
                                     @Param("idGrossiste") Integer idGrossiste);
}