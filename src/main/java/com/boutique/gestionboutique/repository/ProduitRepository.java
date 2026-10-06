package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Produit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ProduitRepository extends JpaRepository<Produit, Integer> {

    // ---------- Listes (la catégorie est chargée avec le produit) ----------

    @EntityGraph(attributePaths = "categorie")
    List<Produit> findAllByOrderByNomAsc();

    @EntityGraph(attributePaths = "categorie")
    List<Produit> findByActifTrueOrderByNomAsc();

    /** Recherche par nom : partielle et insensible à la casse. */
    @EntityGraph(attributePaths = "categorie")
    List<Produit> findByNomContainingIgnoreCaseOrderByNomAsc(String nom);

    /** Même recherche, limitée aux produits actifs (écran du caissier, ventes). */
    @EntityGraph(attributePaths = "categorie")
    List<Produit> findByActifTrueAndNomContainingIgnoreCaseOrderByNomAsc(String nom);

    // ---------- Contrôles d'unicité et de rattachement ----------

    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndIdNot(String nom, Integer id);

    boolean existsByCategorieId(Integer idCategorie);

    long countByCategorieId(Integer idCategorie);

    // ---------- Alertes (tableau de bord, plus tard) ----------

    /** Produits actifs dont le stock est au niveau du seuil d'alerte ou en dessous. */
    @EntityGraph(attributePaths = "categorie")
    @Query("select p from Produit p where p.actif = true and p.quantiteStock <= p.seuilAlerte order by p.quantiteStock asc")
    List<Produit> findProduitsEnAlerte();

    /** Produits actifs dont la péremption est au plus tard à la date donnée. */
    @EntityGraph(attributePaths = "categorie")
    List<Produit> findByActifTrueAndDatePeremptionLessThanEqualOrderByDatePeremptionAsc(LocalDate date);
}