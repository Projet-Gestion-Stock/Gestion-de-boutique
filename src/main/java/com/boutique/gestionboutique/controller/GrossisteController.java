package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.dto.GrossisteDTO;
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
        model.addAttribute("grossistes", grossisteService.lister());
        return "grossistes/liste";
    }

    // ---------- Création ----------

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("grossisteForm", GrossisteDTO.vide());
        preparerFormulaire(model, null);
        return "grossistes/formulaire";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("grossisteForm") GrossisteDTO form,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirect) {
        verifierSaisie(form, result, null);
        if (result.hasErrors()) {
            preparerFormulaire(model, null);
            return "grossistes/formulaire";
        }
        grossisteService.creer(form);
        redirect.addFlashAttribute("succes", "Grossiste « " + form.nom().trim() + " » ajouté.");
        return "redirect:/grossistes";
    }

    // ---------- Modification ----------

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Integer id, Model model) {
        model.addAttribute("grossisteForm", GrossisteDTO.depuis(grossisteService.trouver(id)));
        preparerFormulaire(model, id);
        return "grossistes/formulaire";
    }

    @PostMapping("/{id}")
    public String enregistrerModification(@PathVariable Integer id,
                                          @Valid @ModelAttribute("grossisteForm") GrossisteDTO form,
                                          BindingResult result,
                                          Model model,
                                          RedirectAttributes redirect) {
        verifierSaisie(form, result, id);
        if (result.hasErrors()) {
            preparerFormulaire(model, id);
            return "grossistes/formulaire";
        }
        grossisteService.modifier(id, form);
        redirect.addFlashAttribute("succes", "Grossiste modifié avec succès.");
        return "redirect:/grossistes";
    }

    // ---------- Outils ----------

    /** idGrossiste = null pour une création, l'id du grossiste pour une modification. */
    private void preparerFormulaire(Model model, Integer idGrossiste) {
        model.addAttribute("edition", idGrossiste != null);
        model.addAttribute("idGrossiste", idGrossiste);
    }

    private void verifierSaisie(GrossisteDTO form, BindingResult result, Integer idExclu) {
        if (!result.hasFieldErrors("nom") && grossisteService.nomDejaUtilise(form.nom(), idExclu)) {
            result.rejectValue("nom", "unique", "Un grossiste porte déjà ce nom.");
        }
    }
}
