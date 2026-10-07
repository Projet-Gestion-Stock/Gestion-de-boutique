package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.ProduitDTO;
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

    /** image = fichier envoyé (peut être null ou vide : le produit utilisera alors l'image par défaut). */
    @Transactional
    public Produit creer(ProduitDTO form, MultipartFile image) {
        Produit p = new Produit();
        p.setNom(form.nom().trim());
        p.setPrixVente(form.prixVente());
        p.setQuantiteStock(form.quantiteStock() != null ? form.quantiteStock() : 0);
        p.setSeuilAlerte(form.seuilAlerte());
        p.setDatePeremption(form.datePeremption());
        p.setCategorie(trouverCategorie(form.categorieId()));
        p.setActif(true);

        String nouvelleImage = stockageImageService.enregistrer(image);
        p.setImage(nouvelleImage);
        try {
            return produitRepository.save(p);
        } catch (RuntimeException e) {
            stockageImageService.supprimer(nouvelleImage); // pas de fichier orphelin
            throw e;
        }
    }

    /**
     * Règle P6 : le stock n'est JAMAIS modifié ici (il varie par les livraisons et les achats).
     * Image : un nouveau fichier remplace l'ancien ; supprimerImage = true revient à l'image par défaut.
     */
    @Transactional
    public Produit modifier(Integer id, ProduitDTO form, MultipartFile image, boolean supprimerImage) {
        Produit p = trouver(id);
        p.setNom(form.nom().trim());
        p.setPrixVente(form.prixVente());
        p.setSeuilAlerte(form.seuilAlerte());
        p.setDatePeremption(form.datePeremption());
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
            stockageImageService.supprimer(ancienneImage); // l'ancien fichier n'est plus utilisé
        }
        return enregistre;
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