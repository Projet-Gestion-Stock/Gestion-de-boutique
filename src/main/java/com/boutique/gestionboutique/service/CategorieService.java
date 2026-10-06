package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.CategorieDTO;
import com.boutique.gestionboutique.dto.CategorieResume;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.exception.RessourceIntrouvableException;
import com.boutique.gestionboutique.model.Categorie;
import com.boutique.gestionboutique.repository.CategorieRepository;
import com.boutique.gestionboutique.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final ProduitRepository produitRepository;

    public CategorieService(CategorieRepository categorieRepository,
                            ProduitRepository produitRepository) {
        this.categorieRepository = categorieRepository;
        this.produitRepository = produitRepository;
    }

    /** Liste des catégories avec leur nombre de produits (écran de gestion). */
    @Transactional(readOnly = true)
    public List<CategorieResume> lister() {
        return categorieRepository.findAllByOrderByNomAsc().stream()
                .map(c -> new CategorieResume(c.getId(), c.getNom(),
                        produitRepository.countByCategorieId(c.getId())))
                .toList();
    }

    /** Toutes les catégories, pour les listes déroulantes. */
    @Transactional(readOnly = true)
    public List<Categorie> listerToutes() {
        return categorieRepository.findAllByOrderByNomAsc();
    }

    @Transactional(readOnly = true)
    public Categorie trouver(Integer id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Catégorie introuvable : " + id));
    }

    /** idExclu = id de la catégorie renommée (null à la création). */
    @Transactional(readOnly = true)
    public boolean nomDejaUtilise(String nom, Integer idExclu) {
        String valeur = nom.trim();
        return idExclu == null
                ? categorieRepository.existsByNomIgnoreCase(valeur)
                : categorieRepository.existsByNomIgnoreCaseAndIdNot(valeur, idExclu);
    }

    @Transactional
    public Categorie creer(CategorieDTO form) {
        Categorie c = new Categorie();
        c.setNom(form.nom().trim());
        return categorieRepository.save(c);
    }

    @Transactional
    public Categorie renommer(Integer id, CategorieDTO form) {
        Categorie c = trouver(id);
        c.setNom(form.nom().trim());
        return categorieRepository.save(c);
    }

    /** Règle C3 : une catégorie qui contient des produits ne peut pas être supprimée. */
    @Transactional
    public void supprimer(Integer id) {
        Categorie c = trouver(id);
        if (produitRepository.existsByCategorieId(id)) {
            throw new RegleGestionException(
                    "Impossible de supprimer « " + c.getNom() + " » : cette catégorie contient des produits.");
        }
        categorieRepository.delete(c);
    }
}
