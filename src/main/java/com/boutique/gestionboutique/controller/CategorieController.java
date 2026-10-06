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

    // ---------- Liste ----------

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("categories", categorieService.lister());
        return "categories/liste";
    }

    // ---------- Création ----------

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("categorieForm", CategorieDTO.vide());
        preparerFormulaire(model, null);
        return "categories/formulaire";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("categorieForm") CategorieDTO form,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirect) {
        verifierSaisie(form, result, null);
        if (result.hasErrors()) {
            preparerFormulaire(model, null);
            return "categories/formulaire";
        }
        categorieService.creer(form);
        redirect.addFlashAttribute("succes", "Catégorie « " + form.nom().trim() + " » ajoutée.");
        return "redirect:/categories";
    }

    // ---------- Renommage ----------

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Integer id, Model model) {
        model.addAttribute("categorieForm", CategorieDTO.depuis(categorieService.trouver(id)));
        preparerFormulaire(model, id);
        return "categories/formulaire";
    }

    @PostMapping("/{id}")
    public String enregistrerModification(@PathVariable Integer id,
                                          @Valid @ModelAttribute("categorieForm") CategorieDTO form,
                                          BindingResult result,
                                          Model model,
                                          RedirectAttributes redirect) {
        verifierSaisie(form, result, id);
        if (result.hasErrors()) {
            preparerFormulaire(model, id);
            return "categories/formulaire";
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

    /** idCategorie = null pour une création, l'id de la catégorie pour un renommage. */
    private void preparerFormulaire(Model model, Integer idCategorie) {
        model.addAttribute("edition", idCategorie != null);
        model.addAttribute("idCategorie", idCategorie);
    }

    private void verifierSaisie(CategorieDTO form, BindingResult result, Integer idExclu) {
        if (!result.hasFieldErrors("nom") && categorieService.nomDejaUtilise(form.nom(), idExclu)) {
            result.rejectValue("nom", "unique", "Une catégorie porte déjà ce nom.");
        }
    }
}