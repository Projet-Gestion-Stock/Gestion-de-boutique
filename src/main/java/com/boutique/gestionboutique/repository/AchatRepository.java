package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Achat;
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

public interface AchatRepository extends JpaRepository<Achat, Integer> {

    @EntityGraph(attributePaths = {"utilisateur", "lignes", "lignes.produit"})
    Optional<Achat> findWithDetailsById(Integer id);

    // ---------- Statistiques (tableau de bord) ----------

    @Query("select count(a) from Achat a where a.dateAchat >= :debut and a.dateAchat < :fin")
    long compterEntre(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    // ---------- Historique filtré et paginé ----------

    @EntityGraph(attributePaths = "utilisateur")
    Page<Achat> findByDateAchatGreaterThanEqualAndDateAchatLessThan(
            LocalDateTime debut, LocalDateTime fin, Pageable pageable);

    @EntityGraph(attributePaths = "utilisateur")
    Page<Achat> findByDateAchatGreaterThanEqualAndDateAchatLessThanAndUtilisateurId(
            LocalDateTime debut, LocalDateTime fin, Integer idUtilisateur, Pageable pageable);

    @EntityGraph(attributePaths = "lignes")
    @Query("select a from Achat a where a.id in :ids")
    List<Achat> chargerLignes(@Param("ids") Collection<Integer> ids);

    // ---------- Export ----------

    @EntityGraph(attributePaths = "utilisateur")
    List<Achat> findByDateAchatGreaterThanEqualAndDateAchatLessThan(
            LocalDateTime debut, LocalDateTime fin, Sort sort);

    @EntityGraph(attributePaths = "utilisateur")
    List<Achat> findByDateAchatGreaterThanEqualAndDateAchatLessThanAndUtilisateurId(
            LocalDateTime debut, LocalDateTime fin, Integer idUtilisateur, Sort sort);
}