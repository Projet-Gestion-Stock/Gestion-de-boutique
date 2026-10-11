package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.AchatDTO;
import com.boutique.gestionboutique.dto.LigneAchatDTO;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.exception.RessourceIntrouvableException;
import com.boutique.gestionboutique.model.Achat;
import com.boutique.gestionboutique.model.LigneLivraison;
import com.boutique.gestionboutique.model.Produit;
import com.boutique.gestionboutique.model.Utilisateur;
import com.boutique.gestionboutique.repository.*;
import com.boutique.gestionboutique.util.Periode;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.boutique.gestionboutique.repository.LigneAchatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AchatService {

    private static final Sort TRI = Sort.by(Sort.Order.desc("dateAchat"), Sort.Order.desc("id"));

    private final AchatRepository achatRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final LigneLivraisonRepository ligneLivraisonRepository;
    private final LigneAchatRepository ligneAchatRepository;

    /**
     * Ordre de vente des lots (règle LOT3) : le lot qui périme le plus tôt part en premier ;
     * les lots sans date passent après ; à égalité, le lot reçu en premier (livraison la plus ancienne).
     */
    private static final Comparator<LigneLivraison> ORDRE_DE_VENTE = (a, b) -> {
        LocalDate da = a.getDatePeremption();
        LocalDate db = b.getDatePeremption();
        if (da == null && db != null) return 1;
        if (da != null && db == null) return -1;
        if (da != null && !da.equals(db)) return da.compareTo(db);
        return a.getId().getIdLivraison().compareTo(b.getId().getIdLivraison());
    };

    public AchatService(AchatRepository achatRepository,
                        ProduitRepository produitRepository,
                        UtilisateurRepository utilisateurRepository,
                        LigneLivraisonRepository ligneLivraisonRepository,
                        LigneAchatRepository ligneAchatRepository) {
        this.achatRepository = achatRepository;
        this.produitRepository = produitRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.ligneLivraisonRepository = ligneLivraisonRepository;
        this.ligneAchatRepository = ligneAchatRepository;
    }

    // ---------- Consultation ----------

    @Transactional(readOnly = true)
    public List<Achat> lister() {
        return achatRepository.findAllByOrderByDateAchatDesc();
    }

    @Transactional(readOnly = true)
    public Achat trouverDetaille(Integer id) {
        return achatRepository.findWithDetailsById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Achat introuvable : " + id));
    }

    /** Historique filtré et paginé : période (bornes facultatives, jours inclus) et caissier facultatif. Page 1 = première. */
    @Transactional(readOnly = true)
    public Page<Achat> rechercher(LocalDate du, LocalDate au, Integer caissierId, int page, int taille) {
        Periode periode = Periode.entre(du, au);
        Page<Achat> resultat = chercherPage(periode, caissierId, PageRequest.of(Math.max(page, 1) - 1, taille, TRI));
        // Page demandée au-delà de la dernière : on affiche la dernière
        if (resultat.getTotalPages() > 0 && resultat.getNumber() >= resultat.getTotalPages()) {
            resultat = chercherPage(periode, caissierId, PageRequest.of(resultat.getTotalPages() - 1, taille, TRI));
        }
        List<Integer> ids = resultat.getContent().stream().map(Achat::getId).toList();
        if (!ids.isEmpty()) {
            achatRepository.chargerLignes(ids);   // initialise les lignes des achats de la page
        }
        return resultat;
    }

    private Page<Achat> chercherPage(Periode periode, Integer caissierId, Pageable pageable) {
        return caissierId == null
                ? achatRepository.findByDateAchatGreaterThanEqualAndDateAchatLessThan(
                periode.debut(), periode.fin(), pageable)
                : achatRepository.findByDateAchatGreaterThanEqualAndDateAchatLessThanAndUtilisateurId(
                periode.debut(), periode.fin(), caissierId, pageable);
    }

    /** Règle FI4 : total de TOUS les achats du filtre (pas seulement de la page affichée). */
    @Transactional(readOnly = true)
    public BigDecimal totalFiltre(LocalDate du, LocalDate au, Integer caissierId) {
        Periode periode = Periode.entre(du, au);
        BigDecimal total = caissierId == null
                ? ligneAchatRepository.chiffreAffairesEntre(periode.debut(), periode.fin())
                : ligneAchatRepository.chiffreAffairesEntreParCaissier(periode.debut(), periode.fin(), caissierId);
        return total == null ? BigDecimal.ZERO : total;
    }

    /** Utilisateurs proposés dans le filtre « caissier ». */
    @Transactional(readOnly = true)
    public List<Utilisateur> listerUtilisateurs() {
        return utilisateurRepository.findAllByOrderByNomAscPrenomAsc();
    }

    // ---------- Enregistrement ----------

    /**
     * Enregistre un achat et DIMINUE le stock de chaque produit vendu, en une seule transaction.
     * Les unités sont prélevées lot par lot, la péremption la plus proche d'abord ; un lot périmé n'est jamais vendu.
     * Si un produit est indisponible ou insuffisant, rien n'est enregistré : le message liste tous les
     * problèmes, un par ligne (séparés par des retours à la ligne).
     */
    @Transactional
    public Achat enregistrer(AchatDTO form, String identifiantCaissier) {

        // Règle AC3 : les lignes d'un même produit sont regroupées (quantités additionnées), ordre de saisie conservé
        Map<Integer, Integer> quantites = new LinkedHashMap<>();
        for (LigneAchatDTO ligne : form.lignes()) {
            quantites.merge(ligne.produitId(), ligne.quantite(), Integer::sum);
        }

        Utilisateur caissier = utilisateurRepository.findByIdentifiant(identifiantCaissier)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + identifiantCaissier));

        // Règle AC6 : produits verrouillés jusqu'à la fin de la transaction
        Map<Integer, Produit> produits = produitRepository.findAllByIdForUpdate(quantites.keySet()).stream()
                .collect(Collectors.toMap(Produit::getId, Function.identity()));

        // Lots encore en stock des produits vendus. Lus APRÈS le verrou des produits : plus personne
        // ne peut les modifier avant la fin de la transaction (livraison, vente et retrait verrouillent d'abord le produit).
        Map<Integer, List<LigneLivraison>> lotsParProduit = ligneLivraisonRepository
                .findByProduitIdInAndQuantiteRestanteGreaterThan(quantites.keySet(), 0).stream()
                .collect(Collectors.groupingBy(lot -> lot.getId().getIdProduit()));

        // Règles AC4, AC5 et LOT4 : on contrôle TOUT avant de modifier quoi que ce soit.
        // Seules les unités des lots NON périmés sont vendables.
        List<String> problemes = new ArrayList<>();
        for (Map.Entry<Integer, Integer> ligne : quantites.entrySet()) {
            Produit produit = produits.get(ligne.getKey());
            if (produit == null) {
                problemes.add("Produit introuvable : " + ligne.getKey() + ".");
            } else if (!produit.isActif()) {
                problemes.add("« " + produit.getNom() + " » n'est plus disponible à la vente.");
            } else {
                List<LigneLivraison> lots = lotsParProduit.getOrDefault(ligne.getKey(), List.of());
                int vendable = lots.stream().filter(lot -> !lot.isPerime())
                        .mapToInt(LigneLivraison::getQuantiteRestante).sum();
                int perimees = lots.stream().filter(LigneLivraison::isPerime)
                        .mapToInt(LigneLivraison::getQuantiteRestante).sum();
                if (ligne.getValue() > vendable) {
                    problemes.add("Stock insuffisant pour « " + produit.getNom() + " » : demandé "
                            + ligne.getValue() + ", disponible " + vendable
                            + (perimees > 0 ? " (" + perimees + " unité(s) périmée(s) non vendable(s))" : "")
                            + ".");
                }
            }
        }
        if (!problemes.isEmpty()) {
            throw new RegleGestionException(String.join("\n", problemes));
        }

        // Tout est valide : on enregistre l'achat et on diminue les stocks
        Achat achat = new Achat();
        achat.setUtilisateur(caissier);
        achat.setNomClient(videEnNull(form.nomClient()));
        for (Map.Entry<Integer, Integer> ligne : quantites.entrySet()) {
            Produit produit = produits.get(ligne.getKey());
            achat.ajouterLigne(produit, ligne.getValue());   // règle AC7 : prix de vente copié ici
            prelever(lotsParProduit.get(ligne.getKey()), ligne.getValue());   // règle LOT3 : lot par lot
            produit.setQuantiteStock(produit.getQuantiteStock() - ligne.getValue());   // règle LOT5 : total du produit
        }
        return achatRepository.save(achat);
    }

    /**
     * Retire 'quantite' unités des lots non périmés, dans l'ordre de vente (péremption la plus proche d'abord).
     * La disponibilité a été vérifiée avant l'appel.
     */
    private void prelever(List<LigneLivraison> lots, int quantite) {
        List<LigneLivraison> vendables = lots.stream()
                .filter(lot -> !lot.isPerime())
                .sorted(ORDRE_DE_VENTE)
                .toList();
        int reste = quantite;
        for (LigneLivraison lot : vendables) {
            if (reste == 0) {
                break;
            }
            int prise = Math.min(lot.getQuantiteRestante(), reste);
            lot.setQuantiteRestante(lot.getQuantiteRestante() - prise);
            reste -= prise;
        }
    }

    /** Un nom de client laissé vide est enregistré NULL (et non une chaîne vide). */
    private String videEnNull(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }

    /** Tous les achats du filtre (sans pagination), lignes chargées : pour l'export PDF. */
    @Transactional(readOnly = true)
    public List<Achat> listerPourExport(LocalDate du, LocalDate au, Integer caissierId) {
        Periode periode = Periode.entre(du, au);
        List<Achat> achats = caissierId == null
                ? achatRepository.findByDateAchatGreaterThanEqualAndDateAchatLessThan(
                periode.debut(), periode.fin(), TRI)
                : achatRepository.findByDateAchatGreaterThanEqualAndDateAchatLessThanAndUtilisateurId(
                periode.debut(), periode.fin(), caissierId, TRI);
        List<Integer> ids = achats.stream().map(Achat::getId).toList();
        if (!ids.isEmpty()) {
            achatRepository.chargerLignes(ids);
        }
        return achats;
    }

    /** Libellé du filtre « caissier » pour l'en-tête du PDF. */
    @Transactional(readOnly = true)
    public String libelleCaissier(Integer caissierId) {
        if (caissierId == null) {
            return "Tous les caissiers";
        }
        return utilisateurRepository.findById(caissierId)
                .map(Utilisateur::getNomComplet)
                .orElse("Inconnu");
    }
}