package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.dto.ProduitVenduDTO;
import com.boutique.gestionboutique.model.LigneAchat;
import com.boutique.gestionboutique.model.LigneAchatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface LigneAchatRepository extends JpaRepository<LigneAchat, LigneAchatId> {

    /** Chiffre d'affaires de l'intervalle [debut, fin[ ; renvoie null s'il n'y a aucune vente. */
    @Query("select sum(l.quantite * l.prixUnitaire) from LigneAchat l "
            + "where l.achat.dateAchat >= :debut and l.achat.dateAchat < :fin")
    BigDecimal chiffreAffairesEntre(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /** Produits les plus vendus depuis une date, par quantité décroissante (limiter avec Pageable). */
    @Query("select new com.boutique.gestionboutique.dto.ProduitVenduDTO(p.nom, sum(l.quantite), sum(l.quantite * l.prixUnitaire)) "
            + "from LigneAchat l join l.produit p "
            + "where l.achat.dateAchat >= :debut "
            + "group by p.id, p.nom "
            + "order by sum(l.quantite) desc, sum(l.quantite * l.prixUnitaire) desc")
    List<ProduitVenduDTO> produitsLesPlusVendus(@Param("debut") LocalDateTime debut, Pageable pageable);
}
