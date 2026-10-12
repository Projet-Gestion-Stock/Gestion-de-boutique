package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.form.AchatForm;
import com.boutique.gestionboutique.form.LigneAchatForm;
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
    public Achat trouverDetaille(Integer id) {
        return achatRepository.findWithDetailsById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Achat introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public Page<Achat> rechercher(LocalDate du, LocalDate au, Integer caissierId, int page, int taille) {
        Periode periode = Periode.entre(du, au);
        Page<Achat> resultat = chercherPage(periode, caissierId, PageRequest.of(Math.max(page, 1) - 1, taille, TRI));
        if (resultat.getTotalPages() > 0 && resultat.getNumber() >= resultat.getTotalPages()) {
            resultat = chercherPage(periode, caissierId, PageRequest.of(resultat.getTotalPages() - 1, taille, TRI));
        }
        List<Integer> ids = resultat.getContent().stream().map(Achat::getId).toList();
        if (!ids.isEmpty()) {
            achatRepository.chargerLignes(ids);
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

    @Transactional(readOnly = true)
    public BigDecimal totalFiltre(LocalDate du, LocalDate au, Integer caissierId) {
        Periode periode = Periode.entre(du, au);
        BigDecimal total = caissierId == null
                ? ligneAchatRepository.chiffreAffairesEntre(periode.debut(), periode.fin())
                : ligneAchatRepository.chiffreAffairesEntreParCaissier(periode.debut(), periode.fin(), caissierId);
        return total == null ? BigDecimal.ZERO : total;
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> listerUtilisateurs() {
        return utilisateurRepository.findAllByOrderByNomAscPrenomAsc();
    }

    // ---------- Enregistrement ----------

    @Transactional
    public Achat enregistrer(AchatForm form, String identifiantCaissier) {

        // les lignes d'un même produit sont regroupées (quantités additionnées), ordre de saisie conservé
        Map<Integer, Integer> quantites = new LinkedHashMap<>();
        for (LigneAchatForm ligne : form.lignes()) {
            quantites.merge(ligne.produitId(), ligne.quantite(), Integer::sum);
        }

        Utilisateur caissier = utilisateurRepository.findByIdentifiant(identifiantCaissier)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + identifiantCaissier));

        Map<Integer, Produit> produits = produitRepository.findAllByIdForUpdate(quantites.keySet()).stream()
                .collect(Collectors.toMap(Produit::getId, Function.identity()));

        Map<Integer, List<LigneLivraison>> lotsParProduit = ligneLivraisonRepository
                .findByProduitIdInAndQuantiteRestanteGreaterThan(quantites.keySet(), 0).stream()
                .collect(Collectors.groupingBy(lot -> lot.getId().getIdProduit()));

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
            achat.ajouterLigne(produit, ligne.getValue());
            prelever(lotsParProduit.get(ligne.getKey()), ligne.getValue());
            produit.setQuantiteStock(produit.getQuantiteStock() - ligne.getValue());
        }
        return achatRepository.save(achat);
    }

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

    private String videEnNull(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }

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