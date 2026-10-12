package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.form.LigneLivraisonForm;
import com.boutique.gestionboutique.form.LivraisonForm;
import com.boutique.gestionboutique.form.ProduitForm;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.exception.RessourceIntrouvableException;
import com.boutique.gestionboutique.model.Grossiste;
import com.boutique.gestionboutique.model.Livraison;
import com.boutique.gestionboutique.model.Produit;
import com.boutique.gestionboutique.model.Utilisateur;
import com.boutique.gestionboutique.repository.GrossisteRepository;
import com.boutique.gestionboutique.repository.LigneLivraisonRepository;
import com.boutique.gestionboutique.repository.LivraisonRepository;
import com.boutique.gestionboutique.repository.ProduitRepository;
import com.boutique.gestionboutique.repository.UtilisateurRepository;
import com.boutique.gestionboutique.util.Periode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LivraisonService {

    private static final Sort TRI = Sort.by(Sort.Order.desc("dateLivraison"), Sort.Order.desc("id"));

    private final LivraisonRepository livraisonRepository;
    private final LigneLivraisonRepository ligneLivraisonRepository;
    private final GrossisteRepository grossisteRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProduitService produitService;

    public LivraisonService(LivraisonRepository livraisonRepository,
                            LigneLivraisonRepository ligneLivraisonRepository,
                            GrossisteRepository grossisteRepository,
                            ProduitRepository produitRepository,
                            UtilisateurRepository utilisateurRepository,
                            ProduitService produitService) {
        this.livraisonRepository = livraisonRepository;
        this.ligneLivraisonRepository = ligneLivraisonRepository;
        this.grossisteRepository = grossisteRepository;
        this.produitRepository = produitRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.produitService = produitService;
    }

    // ---------- Consultation ----------

    @Transactional(readOnly = true)
    public Livraison trouverDetaillee(Integer id) {
        return livraisonRepository.findWithDetailsById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Livraison introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public Page<Livraison> rechercher(LocalDate du, LocalDate au, Integer grossisteId, int page, int taille) {
        Periode periode = Periode.entre(du, au);
        Page<Livraison> resultat = chercherPage(periode, grossisteId, PageRequest.of(Math.max(page, 1) - 1, taille, TRI));
        if (resultat.getTotalPages() > 0 && resultat.getNumber() >= resultat.getTotalPages()) {
            resultat = chercherPage(periode, grossisteId, PageRequest.of(resultat.getTotalPages() - 1, taille, TRI));
        }
        List<Integer> ids = resultat.getContent().stream().map(Livraison::getId).toList();
        if (!ids.isEmpty()) {
            livraisonRepository.chargerLignes(ids);   // initialise les lignes des livraisons de la page
        }
        return resultat;
    }

    private Page<Livraison> chercherPage(Periode periode, Integer grossisteId, Pageable pageable) {
        return grossisteId == null
                ? livraisonRepository.findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThan(
                periode.debut(), periode.fin(), pageable)
                : livraisonRepository.findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThanAndGrossisteId(
                periode.debut(), periode.fin(), grossisteId, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalFiltre(LocalDate du, LocalDate au, Integer grossisteId) {
        Periode periode = Periode.entre(du, au);
        BigDecimal total = grossisteId == null
                ? ligneLivraisonRepository.coutEntre(periode.debut(), periode.fin())
                : ligneLivraisonRepository.coutEntreParGrossiste(periode.debut(), periode.fin(), grossisteId);
        return total == null ? BigDecimal.ZERO : total;
    }

    // ---------- Enregistrement ----------

    @Transactional
    public Livraison enregistrer(LivraisonForm form, String identifiantUtilisateur) {
        List<LigneLivraisonForm> lignes = form.lignes();

        // Lignes « produit existant » :
        List<Integer> ids = new ArrayList<>();
        for (LigneLivraisonForm ligne : lignes) {
            if (!ligne.nouveau()) {
                if (ligne.produitId() == null) {
                    throw new RegleGestionException("Choisissez un produit pour chaque ligne.");
                }
                ids.add(ligne.produitId());
            }
        }
        if (ids.stream().distinct().count() != ids.size()) {
            throw new RegleGestionException("Un produit ne peut figurer qu'une seule fois dans une livraison.");
        }

        // Lignes « nouveau produit » : nom unique en base ET entre les lignes de la livraison
        Set<String> nomsVus = new HashSet<>();
        for (LigneLivraisonForm ligne : lignes) {
            if (!ligne.nouveau()) {
                continue;
            }
            ProduitForm np = ligne.nouveauProduit();
            if (np.nom() == null || np.nom().isBlank() || np.prixVente() == null
                    || np.seuilAlerte() == null || np.categorieId() == null) {
                throw new RegleGestionException("Un nouveau produit est incomplet : nom, catégorie, prix de vente et seuil sont obligatoires.");
            }
            String nom = np.nom().trim();
            if (!nomsVus.add(nom.toLowerCase(Locale.ROOT))) {
                throw new RegleGestionException("Le nom « " + nom + " » apparaît deux fois dans cette livraison.");
            }
            if (produitService.nomDejaUtilise(nom, null)) {
                throw new RegleGestionException("Un produit porte déjà le nom « " + nom + " ».");
            }
        }

        Grossiste grossiste = grossisteRepository.findById(form.grossisteId())
                .orElseThrow(() -> new RessourceIntrouvableException("Grossiste introuvable : " + form.grossisteId()));
        Utilisateur utilisateur = utilisateurRepository.findByIdentifiant(identifiantUtilisateur)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + identifiantUtilisateur));

        Map<Integer, Produit> produits = ids.isEmpty()
                ? Map.of()
                : produitRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Produit::getId, Function.identity()));

        Livraison livraison = new Livraison();
        livraison.setGrossiste(grossiste);
        livraison.setUtilisateur(utilisateur);

        for (LigneLivraisonForm ligne : lignes) {
            Produit produit;
            if (ligne.nouveau()) {
                produit = produitService.creerPourLivraison(ligne.nouveauProduit());
            } else {
                produit = produits.get(ligne.produitId());
                if (produit == null) {
                    throw new RegleGestionException("Produit introuvable : " + ligne.produitId());
                }
                if (!produit.isActif()) {
                    throw new RegleGestionException("Le produit « " + produit.getNom()
                            + " » est désactivé : réactivez-le avant de l'inclure dans une livraison.");
                }
            }
            if (ligne.datePeremption() != null && ligne.datePeremption().isBefore(LocalDate.now())) {
                throw new RegleGestionException("« " + produit.getNom() + " » : la date de péremption est dépassée, "
                        + "un lot périmé ne peut pas être réceptionné.");
            }
            livraison.ajouterLigne(produit, ligne.quantite(), ligne.prixAchat(), ligne.datePeremption());
            produit.setQuantiteStock(produit.getQuantiteStock() + ligne.quantite());
        }

        return livraisonRepository.save(livraison);
    }

    @Transactional(readOnly = true)
    public List<Livraison> listerPourExport(LocalDate du, LocalDate au, Integer grossisteId) {
        Periode periode = Periode.entre(du, au);
        List<Livraison> livraisons = grossisteId == null
                ? livraisonRepository.findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThan(
                periode.debut(), periode.fin(), TRI)
                : livraisonRepository.findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThanAndGrossisteId(
                periode.debut(), periode.fin(), grossisteId, TRI);
        List<Integer> ids = livraisons.stream().map(Livraison::getId).toList();
        if (!ids.isEmpty()) {
            livraisonRepository.chargerLignes(ids);
        }
        return livraisons;
    }

    @Transactional(readOnly = true)
    public String libelleGrossiste(Integer grossisteId) {
        if (grossisteId == null) {
            return "Tous les grossistes";
        }
        return grossisteRepository.findById(grossisteId)
                .map(Grossiste::getNom)
                .orElse("Inconnu");
    }
}