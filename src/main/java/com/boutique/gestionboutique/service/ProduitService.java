package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.ProduitDTO;
import com.boutique.gestionboutique.exception.RessourceIntrouvableException;
import com.boutique.gestionboutique.model.Categorie;
import com.boutique.gestionboutique.model.Produit;
import com.boutique.gestionboutique.repository.CategorieRepository;
import com.boutique.gestionboutique.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;

    public ProduitService(ProduitRepository produitRepository,
                          CategorieRepository categorieRepository) {
        this.produitRepository = produitRepository;
        this.categorieRepository = categorieRepository;
    }

    // ---------- Consultation ----------

    /**
     * Liste ou recherche par nom (partielle, insensible à la casse).
     * Le caissier ne voit que les produits actifs ; l'admin voit aussi les désactivés.
     */
    @Transactional(readOnly = true)
    public List<Produit> rechercher(String q, boolean inclureDesactives) {
        String terme = (q == null) ? "" : q.trim();
        if (terme.isEmpty()) {
            return inclureDesactives
                    ? produitRepository.findAllByOrderByNomAsc()
                    : produitRepository.findByActifTrueOrderByNomAsc();
        }
        return inclureDesactives
                ? produitRepository.findByNomContainingIgnoreCaseOrderByNomAsc(terme)
                : produitRepository.findByActifTrueAndNomContainingIgnoreCaseOrderByNomAsc(terme);
    }

    @Transactional(readOnly = true)
    public Produit trouver(Integer id) {
        return produitRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable : " + id));
    }

    /** Pré-remplit le formulaire de modification (construit dans la transaction : la catégorie est chargée à la demande). */
    @Transactional(readOnly = true)
    public ProduitDTO formulairePour(Integer id) {
        Produit p = trouver(id);
        return new ProduitDTO(
                p.getNom(),
                p.getPrixVente(),
                p.getQuantiteStock(),
                p.getSeuilAlerte(),
                p.getDatePeremption(),
                p.getCategorie().getId());
    }

    @Transactional(readOnly = true)
    public int stockActuel(Integer id) {
        return trouver(id).getQuantiteStock();
    }

    /** idExclu = id du produit modifié (null à la création). */
    @Transactional(readOnly = true)
    public boolean nomDejaUtilise(String nom, Integer idExclu) {
        String valeur = nom.trim();
        return idExclu == null
                ? produitRepository.existsByNomIgnoreCase(valeur)
                : produitRepository.existsByNomIgnoreCaseAndIdNot(valeur, idExclu);
    }

    // ---------- Écriture ----------

    @Transactional
    public Produit creer(ProduitDTO form) {
        Produit p = new Produit();
        p.setNom(form.nom().trim());
        p.setPrixVente(form.prixVente());
        p.setQuantiteStock(form.quantiteStock() != null ? form.quantiteStock() : 0);
        p.setSeuilAlerte(form.seuilAlerte());
        p.setDatePeremption(form.datePeremption());
        p.setCategorie(trouverCategorie(form.categorieId()));
        p.setActif(true);
        return produitRepository.save(p);
    }

    /** Règle P6 : le stock n'est JAMAIS modifié ici (il varie par les livraisons et les achats). */
    @Transactional
    public Produit modifier(Integer id, ProduitDTO form) {
        Produit p = trouver(id);
        p.setNom(form.nom().trim());
        p.setPrixVente(form.prixVente());
        p.setSeuilAlerte(form.seuilAlerte());
        p.setDatePeremption(form.datePeremption());
        p.setCategorie(trouverCategorie(form.categorieId()));
        return produitRepository.save(p);
    }

    /** Règle P7 : on désactive, on ne supprime jamais. */
    @Transactional
    public void changerStatut(Integer id, boolean actif) {
        trouver(id).setActif(actif);
    }

    private Categorie trouverCategorie(Integer idCategorie) {
        return categorieRepository.findById(idCategorie)
                .orElseThrow(() -> new RessourceIntrouvableException("Catégorie introuvable : " + idCategorie));
    }
}
