package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.LigneLivraisonDTO;
import com.boutique.gestionboutique.dto.LivraisonDTO;
import com.boutique.gestionboutique.dto.ProduitDTO;
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
    public List<Livraison> lister() {
        return livraisonRepository.findAllByOrderByDateLivraisonDesc();
    }

    @Transactional(readOnly = true)
    public Livraison trouverDetaillee(Integer id) {
        return livraisonRepository.findWithDetailsById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Livraison introuvable : " + id));
    }

    /** Historique filtré et paginé : période (bornes facultatives, jours inclus) et grossiste facultatif. Page 1 = première. */
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

    /** Règle FI4 : coût d'achat de TOUTES les livraisons du filtre (pas seulement de la page affichée). */
    @Transactional(readOnly = true)
    public BigDecimal totalFiltre(LocalDate du, LocalDate au, Integer grossisteId) {
        Periode periode = Periode.entre(du, au);
        BigDecimal total = grossisteId == null
                ? ligneLivraisonRepository.coutEntre(periode.debut(), periode.fin())
                : ligneLivraisonRepository.coutEntreParGrossiste(periode.debut(), periode.fin(), grossisteId);
        return total == null ? BigDecimal.ZERO : total;
    }

    // ---------- Enregistrement ----------

    /**
     * Enregistre une livraison en une seule transaction : création des nouveaux produits (stock = quantité livrée),
     * création de la livraison et de ses lignes, augmentation du stock des produits existants.
     * Si une ligne échoue, rien n'est enregistré (pas même les nouveaux produits).
     */
    @Transactional
    public Livraison enregistrer(LivraisonDTO form, String identifiantUtilisateur) {
        List<LigneLivraisonDTO> lignes = form.lignes();

        // Lignes « produit existant » : règle L3 (contrôlée aussi ici, indépendamment du formulaire)
        List<Integer> ids = new ArrayList<>();
        for (LigneLivraisonDTO ligne : lignes) {
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

        // Lignes « nouveau produit » : nom unique en base (P1) ET entre les lignes de la livraison
        Set<String> nomsVus = new HashSet<>();
        for (LigneLivraisonDTO ligne : lignes) {
            if (!ligne.nouveau()) {
                continue;
            }
            ProduitDTO np = ligne.nouveauProduit();
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

        // Produits existants verrouillés jusqu'à la fin de la transaction
        Map<Integer, Produit> produits = ids.isEmpty()
                ? Map.of()
                : produitRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Produit::getId, Function.identity()));

        Livraison livraison = new Livraison();
        livraison.setGrossiste(grossiste);
        livraison.setUtilisateur(utilisateur);

        for (LigneLivraisonDTO ligne : lignes) {
            Produit produit;
            if (ligne.nouveau()) {
                // Règle P6 : le produit naît avec un stock de 0, la livraison l'augmente juste après
                produit = produitService.creerPourLivraison(ligne.nouveauProduit());
            } else {
                produit = produits.get(ligne.produitId());
                if (produit == null) {
                    throw new RegleGestionException("Produit introuvable : " + ligne.produitId());
                }
                // Règle L4
                if (!produit.isActif()) {
                    throw new RegleGestionException("Le produit « " + produit.getNom()
                            + " » est désactivé : réactivez-le avant de l'inclure dans une livraison.");
                }
            }
            // Règle LOT2 (contrôlée aussi ici, indépendamment du formulaire)
            if (ligne.datePeremption() != null && ligne.datePeremption().isBefore(LocalDate.now())) {
                throw new RegleGestionException("« " + produit.getNom() + " » : la date de péremption est dépassée, "
                        + "un lot périmé ne peut pas être réceptionné.");
            }
            // Règle LOT1 : la ligne est un lot, avec sa propre date de péremption (facultative)
            livraison.ajouterLigne(produit, ligne.quantite(), ligne.prixAchat(), ligne.datePeremption());
            // Règle L5 : le stock augmente ; l'entité est suivie par JPA, la mise à jour part avec la transaction
            produit.setQuantiteStock(produit.getQuantiteStock() + ligne.quantite());
        }

        return livraisonRepository.save(livraison);
    }
}