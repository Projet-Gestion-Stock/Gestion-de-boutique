package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.LigneLivraisonDTO;
import com.boutique.gestionboutique.dto.LivraisonDTO;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.exception.RessourceIntrouvableException;
import com.boutique.gestionboutique.model.Grossiste;
import com.boutique.gestionboutique.model.Livraison;
import com.boutique.gestionboutique.model.Produit;
import com.boutique.gestionboutique.model.Utilisateur;
import com.boutique.gestionboutique.repository.GrossisteRepository;
import com.boutique.gestionboutique.repository.LivraisonRepository;
import com.boutique.gestionboutique.repository.ProduitRepository;
import com.boutique.gestionboutique.repository.UtilisateurRepository;
import com.boutique.gestionboutique.util.Periode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LivraisonService {

    private final LivraisonRepository livraisonRepository;
    private final GrossisteRepository grossisteRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;

    public LivraisonService(LivraisonRepository livraisonRepository,
                            GrossisteRepository grossisteRepository,
                            ProduitRepository produitRepository,
                            UtilisateurRepository utilisateurRepository) {
        this.livraisonRepository = livraisonRepository;
        this.grossisteRepository = grossisteRepository;
        this.produitRepository = produitRepository;
        this.utilisateurRepository = utilisateurRepository;
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

    /** Historique filtré : période (bornes facultatives, jours inclus) et grossiste facultatif. */
    @Transactional(readOnly = true)
    public List<Livraison> rechercher(LocalDate du, LocalDate au, Integer grossisteId) {
        Periode periode = Periode.entre(du, au);
        return grossisteId == null
                ? livraisonRepository.findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThanOrderByDateLivraisonDesc(
                periode.debut(), periode.fin())
                : livraisonRepository.findByDateLivraisonGreaterThanEqualAndDateLivraisonLessThanAndGrossisteIdOrderByDateLivraisonDesc(
                periode.debut(), periode.fin(), grossisteId);
    }

    // ---------- Enregistrement ----------

    /**
     * Enregistre une livraison et AUGMENTE le stock de chaque produit livré, en une seule transaction.
     * Les produits concernés sont verrouillés jusqu'à la fin de l'opération.
     */
    @Transactional
    public Livraison enregistrer(LivraisonDTO form, String identifiantUtilisateur) {
        List<LigneLivraisonDTO> lignes = form.lignes();

        // Règle L3 (contrôlée aussi ici, indépendamment du formulaire)
        List<Integer> ids = lignes.stream().map(LigneLivraisonDTO::produitId).toList();
        if (ids.stream().distinct().count() != ids.size()) {
            throw new RegleGestionException("Un produit ne peut figurer qu'une seule fois dans une livraison.");
        }

        Grossiste grossiste = grossisteRepository.findById(form.grossisteId())
                .orElseThrow(() -> new RessourceIntrouvableException("Grossiste introuvable : " + form.grossisteId()));
        Utilisateur utilisateur = utilisateurRepository.findByIdentifiant(identifiantUtilisateur)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + identifiantUtilisateur));

        Map<Integer, Produit> produits = produitRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Produit::getId, Function.identity()));

        Livraison livraison = new Livraison();
        livraison.setGrossiste(grossiste);
        livraison.setUtilisateur(utilisateur);

        for (LigneLivraisonDTO ligne : lignes) {
            Produit produit = produits.get(ligne.produitId());
            if (produit == null) {
                throw new RegleGestionException("Produit introuvable : " + ligne.produitId());
            }
            // Règle L4
            if (!produit.isActif()) {
                throw new RegleGestionException("Le produit « " + produit.getNom()
                        + " » est désactivé : réactivez-le avant de l'inclure dans une livraison.");
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