package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Produit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProduitRepository extends JpaRepository<Produit, Integer> {

    // ---------- Listes  ----------

    @EntityGraph(attributePaths = "categorie")
    List<Produit> findAllByOrderByNomAsc();

    @EntityGraph(attributePaths = "categorie")
    List<Produit> findByActifTrueOrderByNomAsc();

    @EntityGraph(attributePaths = "categorie")
    List<Produit> findByNomContainingIgnoreCaseOrderByNomAsc(String nom);

    @EntityGraph(attributePaths = "categorie")
    List<Produit> findByActifTrueAndNomContainingIgnoreCaseOrderByNomAsc(String nom);

    // ---------- Contrôles d'unicité et de rattachement ----------

    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndIdNot(String nom, Integer id);

    boolean existsByCategorieId(Integer idCategorie);

    long countByCategorieId(Integer idCategorie);

    // ---------- Alertes ----------

    @EntityGraph(attributePaths = "categorie")
    @Query("select p from Produit p where p.actif = true and p.quantiteStock <= p.seuilAlerte order by p.quantiteStock asc")
    List<Produit> findProduitsEnAlerte();

    // ---------- Mouvements de stock (livraisons, achats) ----------

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produit p where p.id in :ids order by p.id")
    List<Produit> findAllByIdForUpdate(@Param("ids") Collection<Integer> ids);
}