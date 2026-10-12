package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Livraison;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LivraisonRepository extends JpaRepository<Livraison, Integer> {

    @EntityGraph(attributePaths = {"grossiste", "utilisateur", "lignes", "lignes.produit"})
    Optional<Livraison> findWithDetailsById(Integer id);

    // ---------- Historique filtré et paginé ----------

    @EntityGraph(attributePaths = {"grossiste", "utilisateur"})
    Page<Livraison> findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThan(
            LocalDateTime debut, LocalDateTime fin, Pageable pageable);

    @EntityGraph(attributePaths = {"grossiste", "utilisateur"})
    Page<Livraison> findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThanAndGrossisteId(
            LocalDateTime debut, LocalDateTime fin, Integer idGrossiste, Pageable pageable);

    @EntityGraph(attributePaths = "lignes")
    @Query("select l from Livraison l where l.id in :ids")
    List<Livraison> chargerLignes(@Param("ids") Collection<Integer> ids);

    // ---------- Export  ----------

    @EntityGraph(attributePaths = {"grossiste", "utilisateur"})
    List<Livraison> findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThan(
            LocalDateTime debut, LocalDateTime fin, Sort sort);

    @EntityGraph(attributePaths = {"grossiste", "utilisateur"})
    List<Livraison> findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThanAndGrossisteId(
            LocalDateTime debut, LocalDateTime fin, Integer idGrossiste, Sort sort);
}