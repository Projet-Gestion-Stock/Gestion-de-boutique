package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.LotDTO;
import com.boutique.gestionboutique.dto.ProduitVenduDTO;
import com.boutique.gestionboutique.dto.TableauDeBordDTO;
import com.boutique.gestionboutique.model.Produit;
import com.boutique.gestionboutique.repository.AchatRepository;
import com.boutique.gestionboutique.repository.LigneAchatRepository;
import com.boutique.gestionboutique.repository.ProduitRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

    /** Périodes proposées pour le classement des produits les plus vendus (en jours). */
    public static final List<Integer> PERIODES_AUTORISEES = List.of(7, 30, 90);
    public static final int PERIODE_PAR_DEFAUT = 30;

    private static final int JOURS_PEREMPTION_PROCHE = 30;
    private static final int NOMBRE_PRODUITS_CLASSEMENT = 5;

    private final AchatRepository achatRepository;
    private final LigneAchatRepository ligneAchatRepository;
    private final ProduitRepository produitRepository;
    private final LotService lotService;

    public DashboardService(AchatRepository achatRepository,
                            LigneAchatRepository ligneAchatRepository,
                            ProduitRepository produitRepository,
                            LotService lotService) {
        this.achatRepository = achatRepository;
        this.ligneAchatRepository = ligneAchatRepository;
        this.produitRepository = produitRepository;
        this.lotService = lotService;
    }

    /** Construit le tableau de bord de l'administrateur (toute la boutique). */
    @Transactional(readOnly = true)
    public TableauDeBordDTO construire(int periodeJours) {
        LocalDate aujourdhui = LocalDate.now();
        LocalDateTime debutJour = aujourdhui.atStartOfDay();
        LocalDateTime finJour = debutJour.plusDays(1);
        LocalDateTime debutMois = aujourdhui.withDayOfMonth(1).atStartOfDay();

        long nombreVentes = achatRepository.compterEntre(debutJour, finJour);
        BigDecimal chiffreAffairesJour = zeroSiNull(ligneAchatRepository.chiffreAffairesEntre(debutJour, finJour));
        BigDecimal chiffreAffairesMois = zeroSiNull(
                ligneAchatRepository.chiffreAffairesEntre(debutMois, debutMois.plusMonths(1)));

        List<ProduitVenduDTO> classement = ligneAchatRepository.produitsLesPlusVendus(
                aujourdhui.minusDays(periodeJours).atStartOfDay(),
                PageRequest.of(0, NOMBRE_PRODUITS_CLASSEMENT));

        List<Produit> enAlerte = produitRepository.findProduitsEnAlerte();
        // Règle LOT7 : les alertes de péremption portent sur les LOTS
        List<LotDTO> lotsPeremption = lotService.lotsAEchoir(JOURS_PEREMPTION_PROCHE);

        return new TableauDeBordDTO(nombreVentes, chiffreAffairesJour, chiffreAffairesMois,
                enAlerte, lotsPeremption, classement, periodeJours, JOURS_PEREMPTION_PROCHE);
    }

    /** Une somme sans aucune ligne vaut null en base : on la ramène à 0. */
    private BigDecimal zeroSiNull(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}