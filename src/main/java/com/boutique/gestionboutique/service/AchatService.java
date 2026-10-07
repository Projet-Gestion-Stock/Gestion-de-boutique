package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.AchatDTO;
import com.boutique.gestionboutique.dto.LigneAchatDTO;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.exception.RessourceIntrouvableException;
import com.boutique.gestionboutique.model.Achat;
import com.boutique.gestionboutique.model.Produit;
import com.boutique.gestionboutique.model.Utilisateur;
import com.boutique.gestionboutique.repository.AchatRepository;
import com.boutique.gestionboutique.repository.ProduitRepository;
import com.boutique.gestionboutique.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AchatService {

    private final AchatRepository achatRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AchatService(AchatRepository achatRepository,
                        ProduitRepository produitRepository,
                        UtilisateurRepository utilisateurRepository) {
        this.achatRepository = achatRepository;
        this.produitRepository = produitRepository;
        this.utilisateurRepository = utilisateurRepository;
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

    // ---------- Enregistrement ----------

    /**
     * Enregistre un achat et DIMINUE le stock de chaque produit vendu, en une seule transaction.
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

        // Règles AC4 et AC5 : on contrôle TOUT avant de modifier quoi que ce soit
        List<String> problemes = new ArrayList<>();
        for (Map.Entry<Integer, Integer> ligne : quantites.entrySet()) {
            Produit produit = produits.get(ligne.getKey());
            if (produit == null) {
                problemes.add("Produit introuvable : " + ligne.getKey() + ".");
            } else if (!produit.isActif()) {
                problemes.add("« " + produit.getNom() + " » n'est plus disponible à la vente.");
            } else if (ligne.getValue() > produit.getQuantiteStock()) {
                problemes.add("Stock insuffisant pour « " + produit.getNom() + " » : demandé "
                        + ligne.getValue() + ", disponible " + produit.getQuantiteStock() + ".");
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
            produit.setQuantiteStock(produit.getQuantiteStock() - ligne.getValue());
        }
        return achatRepository.save(achat);
    }

    /** Un nom de client laissé vide est enregistré NULL (et non une chaîne vide). */
    private String videEnNull(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }
}