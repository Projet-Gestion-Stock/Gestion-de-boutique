package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.dto.CategorieDTO;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.service.CategorieService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
public class CategorieController {

    private final CategorieService categorieService;

    public CategorieController(CategorieService categorieService) {
        this.categorieService = categorieService;
    }

    // ---------- Liste (la création et le renommage se font dans une fenêtre) ----------

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("categorieForm", CategorieDTO.vide());
        preparerListe(model, null, false);
        return "categories/liste";
    }

    // ---------- Création ----------

    @PostMapping
    public String creer(@Valid @ModelAttribute("categorieForm") CategorieDTO form,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirect) {
        verifierSaisie(form, result, null);
        if (result.hasErrors()) {
            preparerListe(model, null, true);
            return "categories/liste";
        }
        categorieService.creer(form);
        redirect.addFlashAttribute("succes", "Catégorie « " + form.nom().trim() + " » ajoutée.");
        return "redirect:/categories";
    }

    // ---------- Renommage ----------

    @PostMapping("/{id}")
    public String enregistrerModification(@PathVariable Integer id,
                                          @Valid @ModelAttribute("categorieForm") CategorieDTO form,
                                          BindingResult result,
                                          Model model,
                                          RedirectAttributes redirect) {
        verifierSaisie(form, result, id);
        if (result.hasErrors()) {
            preparerListe(model, id, true);
            return "categories/liste";
        }
        categorieService.renommer(id, form);
        redirect.addFlashAttribute("succes", "Catégorie renommée en « " + form.nom().trim() + " ».");
        return "redirect:/categories";
    }

    // ---------- Suppression (règle C3) ----------

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            categorieService.supprimer(id);
            redirect.addFlashAttribute("succes", "Catégorie supprimée.");
        } catch (RegleGestionException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/categories";
    }

    // ---------- Outils ----------

    /** idCategorie = null pour une création ; ouvrir = true quand la fenêtre doit se rouvrir (formulaire refusé). */
    private void preparerListe(Model model, Integer idCategorie, boolean ouvrir) {
        model.addAttribute("categories", categorieService.lister());
        model.addAttribute("edition", idCategorie != null);
        model.addAttribute("idCategorie", idCategorie);
        model.addAttribute("ouvrirModal", ouvrir);
    }

    private void verifierSaisie(CategorieDTO form, BindingResult result, Integer idExclu) {
        if (!result.hasFieldErrors("nom") && categorieService.nomDejaUtilise(form.nom(), idExclu)) {
            result.rejectValue("nom", "unique", "Une catégorie porte déjà ce nom.");
        }
    }
}