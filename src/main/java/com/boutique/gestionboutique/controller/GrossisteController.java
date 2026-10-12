package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.form.GrossisteForm;
import com.boutique.gestionboutique.service.GrossisteService;
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
@RequestMapping("/grossistes")
public class GrossisteController {

    private final GrossisteService grossisteService;

    public GrossisteController(GrossisteService grossisteService) {
        this.grossisteService = grossisteService;
    }

    // ---------- Liste ----------

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("grossisteForm", GrossisteForm.vide());
        preparerListe(model, null, false);
        return "grossistes/liste";
    }

    // ---------- Création ----------

    @PostMapping
    public String creer(@Valid @ModelAttribute("grossisteForm") GrossisteForm form,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirect) {
        verifierSaisie(form, result, null);
        if (result.hasErrors()) {
            preparerListe(model, null, true);
            return "grossistes/liste";
        }
        grossisteService.creer(form);
        redirect.addFlashAttribute("succes", "Grossiste « " + form.nom().trim() + " » ajouté.");
        return "redirect:/grossistes";
    }

    // ---------- Modification ----------

    @PostMapping("/{id}")
    public String enregistrerModification(@PathVariable Integer id,
                                          @Valid @ModelAttribute("grossisteForm") GrossisteForm form,
                                          BindingResult result,
                                          Model model,
                                          RedirectAttributes redirect) {
        verifierSaisie(form, result, id);
        if (result.hasErrors()) {
            preparerListe(model, id, true);
            return "grossistes/liste";
        }
        grossisteService.modifier(id, form);
        redirect.addFlashAttribute("succes", "Grossiste modifié avec succès.");
        return "redirect:/grossistes";
    }

    // ---------- Outils ----------

    private void preparerListe(Model model, Integer idGrossiste, boolean ouvrir) {
        model.addAttribute("grossistes", grossisteService.lister());
        model.addAttribute("edition", idGrossiste != null);
        model.addAttribute("idGrossiste", idGrossiste);
        model.addAttribute("ouvrirModal", ouvrir);
    }

    private void verifierSaisie(GrossisteForm form, BindingResult result, Integer idExclu) {
        if (!result.hasFieldErrors("nom") && grossisteService.nomDejaUtilise(form.nom(), idExclu)) {
            result.rejectValue("nom", "unique", "Un grossiste porte déjà ce nom.");
        }
    }
}