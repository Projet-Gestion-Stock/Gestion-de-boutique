package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Livraison;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LivraisonRepository extends JpaRepository<Livraison, Integer> {

    /** Historique : de la plus récente à la plus ancienne (les lignes sont chargées pour calculer le total). */
    @EntityGraph(attributePaths = {"grossiste", "utilisateur", "lignes"})
    List<Livraison> findAllByOrderByDateLivraisonDesc();

    /** Une livraison avec tout son détail (grossiste, utilisateur, lignes et produits). */
    @EntityGraph(attributePaths = {"grossiste", "utilisateur", "lignes", "lignes.produit"})
    Optional<Livraison> findWithDetailsById(Integer id);

    // ---------- Historique filtré et paginé ----------

    /** Livraisons de la période [debut, fin[. Le tri vient du Pageable. */
    @EntityGraph(attributePaths = {"grossiste", "utilisateur"})
    Page<Livraison> findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThan(
            LocalDateTime debut, LocalDateTime fin, Pageable pageable);

    /** Même chose, limité aux livraisons d'un grossiste. */
    @EntityGraph(attributePaths = {"grossiste", "utilisateur"})
    Page<Livraison> findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThanAndGrossisteId(
            LocalDateTime debut, LocalDateTime fin, Integer idGrossiste, Pageable pageable);

    /** Charge les lignes des livraisons de la page dans la session en cours (voir AchatRepository.chargerLignes). */
    @EntityGraph(attributePaths = "lignes")
    @Query("select l from Livraison l where l.id in :ids")
    List<Livraison> chargerLignes(@Param("ids") Collection<Integer> ids);
}