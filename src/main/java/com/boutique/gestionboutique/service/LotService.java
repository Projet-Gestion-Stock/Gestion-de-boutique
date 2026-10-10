package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.InfoPeremptionDTO;
import com.boutique.gestionboutique.dto.LotDTO;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.exception.RessourceIntrouvableException;
import com.boutique.gestionboutique.model.LigneLivraison;
import com.boutique.gestionboutique.model.LigneLivraisonId;
import com.boutique.gestionboutique.model.Produit;
import com.boutique.gestionboutique.repository.LigneLivraisonRepository;
import com.boutique.gestionboutique.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Gestion des péremptions par lot : consultation, alertes et retrait des lots périmés. */
@Service
public class LotService {

    private final LigneLivraisonRepository ligneLivraisonRepository;
    private final ProduitRepository produitRepository;

    public LotService(LigneLivraisonRepository ligneLivraisonRepository,
                      ProduitRepository produitRepository) {
        this.ligneLivraisonRepository = ligneLivraisonRepository;
        this.produitRepository = produitRepository;
    }

    // ---------- Consultation ----------

    /** Tous les lots datés encore en stock, du plus proche de la péremption au plus lointain. */
    @Transactional(readOnly = true)
    public List<LotDTO> lotsAvecPeremption() {
        LocalDate aujourdhui = LocalDate.now();
        return ligneLivraisonRepository
                .findByQuantiteRestanteGreaterThanAndDatePeremptionIsNotNullOrderByDatePeremptionAsc(0).stream()
                .map(lot -> versDto(lot, aujourdhui))
                .toList();
    }

    /** Lots périmés ou qui périment dans les 'jours' jours (produits actifs) : alertes du tableau de bord. */
    @Transactional(readOnly = true)
    public List<LotDTO> lotsAEchoir(int jours) {
        LocalDate aujourdhui = LocalDate.now();
        return ligneLivraisonRepository
                .findByQuantiteRestanteGreaterThanAndDatePeremptionLessThanEqualAndProduitActifTrueOrderByDatePeremptionAsc(
                        0, aujourdhui.plusDays(jours)).stream()
                .map(lot -> versDto(lot, aujourdhui))
                .toList();
    }

    /** Lots périmés encore en stock (retrait). */
    @Transactional(readOnly = true)
    public List<LotDTO> lotsPerimes() {
        LocalDate aujourdhui = LocalDate.now();
        return ligneLivraisonRepository
                .findByQuantiteRestanteGreaterThanAndDatePeremptionLessThan(0, aujourdhui).stream()
                .map(lot -> versDto(lot, aujourdhui))
                .toList();
    }

    /** Règle LOT8 : pour chaque produit, la prochaine péremption et le nombre d'unités déjà périmées. */
    @Transactional(readOnly = true)
    public Map<Integer, InfoPeremptionDTO> infosParProduit() {
        LocalDate aujourdhui = LocalDate.now();
        Map<Integer, LocalDate> prochaine = new HashMap<>();
        Map<Integer, Integer> perimees = new HashMap<>();

        for (LigneLivraison lot : ligneLivraisonRepository
                .findByQuantiteRestanteGreaterThanAndDatePeremptionIsNotNullOrderByDatePeremptionAsc(0)) {
            Integer idProduit = lot.getId().getIdProduit();
            if (lot.getDatePeremption().isBefore(aujourdhui)) {
                perimees.merge(idProduit, lot.getQuantiteRestante(), Integer::sum);
            } else {
                prochaine.merge(idProduit, lot.getDatePeremption(), (a, b) -> a.isBefore(b) ? a : b);
            }
        }

        Set<Integer> idsProduits = new HashSet<>(prochaine.keySet());
        idsProduits.addAll(perimees.keySet());

        Map<Integer, InfoPeremptionDTO> infos = new HashMap<>();
        for (Integer id : idsProduits) {
            LocalDate date = prochaine.get(id);
            Long jours = date == null ? null : ChronoUnit.DAYS.between(aujourdhui, date);
            infos.put(id, new InfoPeremptionDTO(date, jours, perimees.getOrDefault(id, 0)));
        }
        return infos;
    }

    // ---------- Retrait des lots périmés (règle LOT6) ----------

    /**
     * Retire du stock les unités restantes d'un lot périmé. Le produit est verrouillé AVANT de lire le lot :
     * une vente simultanée ne peut pas modifier le lot entre-temps.
     * Retourne le nombre d'unités retirées.
     */
    @Transactional
    public int retirer(Integer idLivraison, Integer idProduit) {
        Produit produit = produitRepository.findAllByIdForUpdate(List.of(idProduit)).stream()
                .findFirst()
                .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable : " + idProduit));

        LigneLivraison lot = ligneLivraisonRepository.findById(new LigneLivraisonId(idLivraison, idProduit))
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Lot introuvable : livraison " + idLivraison + ", produit " + idProduit));

        if (!lot.isPerime()) {
            throw new RegleGestionException("Ce lot n'est pas périmé : il ne peut pas être retiré du stock.");
        }
        int quantite = lot.getQuantiteRestante();
        if (quantite == 0) {
            throw new RegleGestionException("Ce lot n'a plus aucune unité en stock.");
        }

        lot.setQuantiteRetiree(lot.getQuantiteRetiree() + quantite);
        lot.setQuantiteRestante(0);
        produit.setQuantiteStock(produit.getQuantiteStock() - quantite);
        return quantite;
    }

    private LotDTO versDto(LigneLivraison lot, LocalDate aujourdhui) {
        return new LotDTO(
                lot.getId().getIdLivraison(),
                lot.getId().getIdProduit(),
                lot.getProduit().getNom(),
                lot.getProduit().getImageUrl(),
                lot.getDatePeremption(),
                lot.getQuantiteRestante(),
                ChronoUnit.DAYS.between(aujourdhui, lot.getDatePeremption()));
    }
}