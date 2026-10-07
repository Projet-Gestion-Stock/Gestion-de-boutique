package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Achat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

}