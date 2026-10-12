package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.form.ProduitForm;
import com.boutique.gestionboutique.exception.RessourceIntrouvableException;
import com.boutique.gestionboutique.model.Categorie;
import com.boutique.gestionboutique.model.Produit;
import com.boutique.gestionboutique.repository.CategorieRepository;
import com.boutique.gestionboutique.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final StockageImageService stockageImageService;

    public ProduitService(ProduitRepository produitRepository,
                          CategorieRepository categorieRepository,
                          StockageImageService stockageImageService) {
        this.produitRepository = produitRepository;
        this.categorieRepository = categorieRepository;
        this.stockageImageService = stockageImageService;
    }

    // ---------- Consultation ----------

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

    @Transactional(readOnly = true)
    public boolean nomDejaUtilise(String nom, Integer idExclu) {
        String valeur = nom.trim();
        return idExclu == null
                ? produitRepository.existsByNomIgnoreCase(valeur)
                : produitRepository.existsByNomIgnoreCaseAndIdNot(valeur, idExclu);
    }

    // ---------- Écriture ----------

    @Transactional
    public Produit creerPourLivraison(ProduitForm form) {
        Produit p = new Produit();
        p.setNom(form.nom().trim());
        p.setPrixVente(form.prixVente());
        p.setQuantiteStock(0);
        p.setSeuilAlerte(form.seuilAlerte());
        p.setCategorie(trouverCategorie(form.categorieId()));
        p.setActif(true);
        return produitRepository.save(p);
    }

    @Transactional
    public Produit modifier(Integer id, ProduitForm form, MultipartFile image, boolean supprimerImage) {
        Produit p = trouver(id);
        p.setNom(form.nom().trim());
        p.setPrixVente(form.prixVente());
        p.setSeuilAlerte(form.seuilAlerte());
        p.setCategorie(trouverCategorie(form.categorieId()));

        String ancienneImage = p.getImage();
        String nouvelleImage = stockageImageService.enregistrer(image);
        if (nouvelleImage != null) {
            p.setImage(nouvelleImage);
        } else if (supprimerImage) {
            p.setImage(null);
        }

        Produit enregistre;
        try {
            enregistre = produitRepository.save(p);
        } catch (RuntimeException e) {
            stockageImageService.supprimer(nouvelleImage);
            throw e;
        }
        if (!Objects.equals(ancienneImage, enregistre.getImage())) {
            stockageImageService.supprimer(ancienneImage); // l'ancienne image n'est plus utilisée
        }
        return enregistre;
    }

    @Transactional
    public void changerStatut(Integer id, boolean actif) {
        trouver(id).setActif(actif);
    }

    private Categorie trouverCategorie(Integer idCategorie) {
        return categorieRepository.findById(idCategorie)
                .orElseThrow(() -> new RessourceIntrouvableException("Catégorie introuvable : " + idCategorie));
    }
}