package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Livraison;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LivraisonRepository extends JpaRepository<Livraison, Integer> {

    /** Historique : de la plus récente à la plus ancienne (les lignes sont chargées pour calculer le total). */
    @EntityGraph(attributePaths = {"grossiste", "utilisateur", "lignes"})
    List<Livraison> findAllByOrderByDateLivraisonDesc();

    /** Une livraison avec tout son détail (grossiste, utilisateur, lignes et produits). */
    @EntityGraph(attributePaths = {"grossiste", "utilisateur", "lignes", "lignes.produit"})
    Optional<Livraison> findWithDetailsById(Integer id);
}
