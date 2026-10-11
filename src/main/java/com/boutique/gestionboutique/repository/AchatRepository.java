package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Achat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AchatRepository extends JpaRepository<Achat, Integer> {

    /** Historique : du plus récent au plus ancien (les lignes sont chargées pour calculer le total). */
    @EntityGraph(attributePaths = {"utilisateur", "lignes"})
    List<Achat> findAllByOrderByDateAchatDesc();

    /** Un achat avec tout son détail (caissier, lignes et produits). */
    @EntityGraph(attributePaths = {"utilisateur", "lignes", "lignes.produit"})
    Optional<Achat> findWithDetailsById(Integer id);

    // ---------- Statistiques (tableau de bord) ----------

    /** Nombre d'achats enregistrés dans l'intervalle [debut, fin[. */
    @Query("select count(a) from Achat a where a.dateAchat >= :debut and a.dateAchat < :fin")
    long compterEntre(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    // ---------- Historique filtré et paginé ----------

    /** Achats de la période [debut, fin[. Le tri vient du Pageable. */
    @EntityGraph(attributePaths = "utilisateur")
    Page<Achat> findByDateAchatGreaterThanEqualAndDateAchatLessThan(
            LocalDateTime debut, LocalDateTime fin, Pageable pageable);

    /** Même chose, limité aux achats d'un caissier. */
    @EntityGraph(attributePaths = "utilisateur")
    Page<Achat> findByDateAchatGreaterThanEqualAndDateAchatLessThanAndUtilisateurId(
            LocalDateTime debut, LocalDateTime fin, Integer idUtilisateur, Pageable pageable);

    /**
     * Charge les lignes des achats donnés dans la session en cours. On ne peut pas les joindre dans la requête
     * paginée (Hibernate paginerait alors en mémoire) : on les charge ensuite, pour la seule page affichée.
     */
    @EntityGraph(attributePaths = "lignes")
    @Query("select a from Achat a where a.id in :ids")
    List<Achat> chargerLignes(@Param("ids") Collection<Integer> ids);

    // ---------- Export (sans pagination) ----------

    @EntityGraph(attributePaths = "utilisateur")
    List<Achat> findByDateAchatGreaterThanEqualAndDateAchatLessThan(
            LocalDateTime debut, LocalDateTime fin, Sort sort);

    @EntityGraph(attributePaths = "utilisateur")
    List<Achat> findByDateAchatGreaterThanEqualAndDateAchatLessThanAndUtilisateurId(
            LocalDateTime debut, LocalDateTime fin, Integer idUtilisateur, Sort sort);
}