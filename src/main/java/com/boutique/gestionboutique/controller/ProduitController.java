package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.dto.ProduitDTO;
import com.boutique.gestionboutique.model.Produit;
import com.boutique.gestionboutique.service.CategorieService;
import com.boutique.gestionboutique.service.ProduitService;
import jakarta.validation.Valid;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/produits")
public class ProduitController {

    /** Un produit est « proche de la péremption » s'il périme dans ce nombre de jours. */
    private static final int JOURS_PEREMPTION_PROCHE = 30;

    private final ProduitService produitService;
    private final CategorieService categorieService;

    public ProduitController(ProduitService produitService, CategorieService categorieService) {
        this.produitService = produitService;
        this.categorieService = categorieService;
    }

    // ---------- Liste et recherche (ADMIN et CAISSIER) ----------

    @GetMapping
    public String liste(@RequestParam(name = "q", required = false) String q,
                        Authentication authentication,
                        Model model) {
        // L'admin voit aussi les produits désactivés ; le caissier uniquement les actifs
        List<Produit> produits = produitService.rechercher(q, estAdmin(authentication));
        model.addAttribute("produits", produits);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("joursPeremption", JOURS_PEREMPTION_PROCHE);
        return "produits/liste";
    }

    // ---------- Création (ADMIN) ----------

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("produitForm", ProduitDTO.vide());
        preparerFormulaire(model, null);
        return "produits/formulaire";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("produitForm") ProduitDTO form,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirect) {
        verifierSaisie(form, result, null);
        if (result.hasErrors()) {
            preparerFormulaire(model, null);
            return "produits/formulaire";
        }
        produitService.creer(form);
        redirect.addFlashAttribute("succes", "Produit « " + form.nom().trim() + " » ajouté.");
        return "redirect:/produits";
    }

    // ---------- Modification (ADMIN) ----------

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Integer id, Model model) {
        model.addAttribute("produitForm", produitService.formulairePour(id));
        preparerFormulaire(model, id);
        return "produits/formulaire";
    }

    @PostMapping("/{id}")
    public String enregistrerModification(@PathVariable Integer id,
                                          @Valid @ModelAttribute("produitForm") ProduitDTO form,
                                          BindingResult result,
                                          Model model,
                                          RedirectAttributes redirect) {
        verifierSaisie(form, result, id);
        if (result.hasErrors()) {
            preparerFormulaire(model, id);
            return "produits/formulaire";
        }
        produitService.modifier(id, form);
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

    /** idProduit = null pour une création, l'id du produit pour une modification. */
    private void preparerFormulaire(Model model, Integer idProduit) {
        model.addAttribute("edition", idProduit != null);
        model.addAttribute("idProduit", idProduit);
        model.addAttribute("categories", categorieService.listerToutes());
        if (idProduit != null) {
            model.addAttribute("stockActuel", produitService.stockActuel(idProduit));
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
