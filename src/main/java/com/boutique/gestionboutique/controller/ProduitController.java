package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.dto.ProduitDTO;
import com.boutique.gestionboutique.model.Produit;
import com.boutique.gestionboutique.service.CategorieService;
import com.boutique.gestionboutique.service.ProduitService;
import com.boutique.gestionboutique.service.StockageImageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/produits")
public class ProduitController {

    /** Un produit est « proche de la péremption » s'il périme dans ce nombre de jours. */
    private static final int JOURS_PEREMPTION_PROCHE = 30;

    private final ProduitService produitService;
    private final CategorieService categorieService;
    private final StockageImageService stockageImageService;
    private final String devise;

    public ProduitController(ProduitService produitService,
                             CategorieService categorieService,
                             StockageImageService stockageImageService,
                             @Value("${app.boutique.devise:}") String devise) {
        this.produitService = produitService;
        this.categorieService = categorieService;
        this.stockageImageService = stockageImageService;
        this.devise = devise;
    }

    // ---------- Liste et recherche (ADMIN et CAISSIER) ----------

    @GetMapping
    public String liste(@RequestParam(name = "q", required = false) String q,
                        Authentication authentication,
                        Model model) {
        model.addAttribute("produitForm", ProduitDTO.vide());
        preparerListe(model, authentication, q);
        preparerEtatFormulaire(model, null, false);
        return "produits/liste";
    }

    // ---------- Création (ADMIN) : la fenêtre se rouvre si le formulaire est refusé ----------

    @PostMapping
    public String creer(@Valid @ModelAttribute("produitForm") ProduitDTO form,
                        BindingResult result,
                        @RequestParam(name = "imageFichier", required = false) MultipartFile imageFichier,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirect) {
        verifierSaisie(form, result, null);
        String erreurImage = stockageImageService.verifier(imageFichier);
        if (result.hasErrors() || erreurImage != null) {
            model.addAttribute("erreurImage", erreurImage);
            preparerListe(model, authentication, null);
            preparerEtatFormulaire(model, null, true);
            return "produits/liste";
        }
        produitService.creer(form, imageFichier);
        redirect.addFlashAttribute("succes", "Produit « " + form.nom().trim() + " » ajouté.");
        return "redirect:/produits";
    }

    // ---------- Modification (ADMIN) ----------

    @PostMapping("/{id}")
    public String enregistrerModification(@PathVariable Integer id,
                                          @Valid @ModelAttribute("produitForm") ProduitDTO form,
                                          BindingResult result,
                                          @RequestParam(name = "imageFichier", required = false) MultipartFile imageFichier,
                                          @RequestParam(name = "supprimerImage", defaultValue = "false") boolean supprimerImage,
                                          Authentication authentication,
                                          Model model,
                                          RedirectAttributes redirect) {
        verifierSaisie(form, result, id);
        String erreurImage = stockageImageService.verifier(imageFichier);
        if (result.hasErrors() || erreurImage != null) {
            model.addAttribute("erreurImage", erreurImage);
            preparerListe(model, authentication, null);
            preparerEtatFormulaire(model, id, true);
            return "produits/liste";
        }
        produitService.modifier(id, form, imageFichier, supprimerImage);
        redirect.addFlashAttribute("succes", "Produit modifié avec succès.");
        return "redirect:/produits";
    }

    // ---------- Activation / désactivation (ADMIN) ----------

    @PostMapping("/{id}/desactiver")
    public String desactiver(@PathVariable Integer id, RedirectAttributes redirect) {
        produitService.changerStatut(id, false);
        redirect.addFlashAttribute("succes", "Produit désactivé.");
        return "redirect:/produits";
    }

    @PostMapping("/{id}/activer")
    public String activer(@PathVariable Integer id, RedirectAttributes redirect) {
        produitService.changerStatut(id, true);
        redirect.addFlashAttribute("succes", "Produit réactivé.");
        return "redirect:/produits";
    }

    // ---------- Outils ----------

    private boolean estAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /** Données de la liste (le caissier ne voit que les produits actifs, l'admin voit aussi les désactivés). */
    private void preparerListe(Model model, Authentication authentication, String q) {
        boolean admin = estAdmin(authentication);
        model.addAttribute("produits", produitService.rechercher(q, admin));
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("joursPeremption", JOURS_PEREMPTION_PROCHE);
        model.addAttribute("devise", devise);
        model.addAttribute("produitVide", ProduitDTO.vide());
        if (admin) {
            model.addAttribute("categories", categorieService.listerToutes());
        }
    }

    /**
     * État de la fenêtre d'ajout / modification.
     * idProduit = null pour une création ; ouvrir = true quand la fenêtre doit s'ouvrir toute seule (formulaire refusé).
     */
    private void preparerEtatFormulaire(Model model, Integer idProduit, boolean ouvrir) {
        model.addAttribute("edition", idProduit != null);
        model.addAttribute("idProduit", idProduit);
        model.addAttribute("ouvrirModal", ouvrir);
        if (idProduit != null) {
            Produit produit = produitService.trouver(idProduit);
            model.addAttribute("stockActuel", produit.getQuantiteStock());
            model.addAttribute("imageUrl", produit.getImageUrl());
            model.addAttribute("imagePerso", produit.getImage() != null);
        } else {
            model.addAttribute("stockActuel", null);
            model.addAttribute("imageUrl", Produit.IMAGE_PAR_DEFAUT);
            model.addAttribute("imagePerso", false);
        }
    }

    /** Contrôles qui dépendent de la base ou du contexte (création / modification). */
    private void verifierSaisie(ProduitDTO form, BindingResult result, Integer idExclu) {
        if (!result.hasFieldErrors("nom") && produitService.nomDejaUtilise(form.nom(), idExclu)) {
            result.rejectValue("nom", "unique", "Un produit porte déjà ce nom.");
        }
        // Règle P6 : le stock initial est obligatoire à la création uniquement
        if (idExclu == null && !result.hasFieldErrors("quantiteStock") && form.quantiteStock() == null) {
            result.rejectValue("quantiteStock", "required", "Le stock initial est obligatoire.");
        }
    }
}