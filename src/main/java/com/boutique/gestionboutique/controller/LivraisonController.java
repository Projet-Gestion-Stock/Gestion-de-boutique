package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.dto.LigneLivraisonDTO;
import com.boutique.gestionboutique.dto.LivraisonDTO;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.model.Livraison;
import com.boutique.gestionboutique.service.GrossisteService;
import com.boutique.gestionboutique.service.LivraisonService;
import com.boutique.gestionboutique.service.ProduitService;
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

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/livraisons")
public class LivraisonController {

    private final LivraisonService livraisonService;
    private final GrossisteService grossisteService;
    private final ProduitService produitService;

    public LivraisonController(LivraisonService livraisonService,
                               GrossisteService grossisteService,
                               ProduitService produitService) {
        this.livraisonService = livraisonService;
        this.grossisteService = grossisteService;
        this.produitService = produitService;
    }

    // ---------- Historique ----------

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("livraisons", livraisonService.lister());
        return "livraisons/liste";
    }

    // ---------- Détail ----------

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("livraison", livraisonService.trouverDetaillee(id));
        return "livraisons/detail";
    }

    // ---------- Saisie ----------

    @GetMapping("/nouvelle")
    public String nouvelle(Model model) {
        model.addAttribute("livraisonForm", LivraisonDTO.vide());
        preparerFormulaire(model);
        return "livraisons/formulaire";
    }

    @PostMapping
    public String enregistrer(@Valid @ModelAttribute("livraisonForm") LivraisonDTO form,
                              BindingResult result,
                              Model model,
                              Principal principal,
                              RedirectAttributes redirect) {
        verifierLignes(form, result);
        if (result.hasErrors()) {
            preparerFormulaire(model);
            return "livraisons/formulaire";
        }
        try {
            Livraison livraison = livraisonService.enregistrer(form, principal.getName());
            redirect.addFlashAttribute("succes",
                    "Livraison n° " + livraison.getId() + " enregistrée : le stock a été mis à jour.");
            return "redirect:/livraisons/" + livraison.getId();
        } catch (RegleGestionException e) {
            result.reject("regle.gestion", e.getMessage());
            preparerFormulaire(model);
            return "livraisons/formulaire";
        }
    }

    // ---------- Outils ----------

    private void preparerFormulaire(Model model) {
        model.addAttribute("grossistes", grossisteService.lister());
        // Règle L4 : seuls les produits actifs sont proposés
        model.addAttribute("produits", produitService.rechercher(null, false));
    }

    /** Règle L3 : un produit ne peut figurer qu'une fois ; l'erreur est placée sur la ligne en doublon. */
    private void verifierLignes(LivraisonDTO form, BindingResult result) {
        if (form.lignes() == null) {
            return; // déjà signalé par @NotEmpty
        }
        Map<Integer, Integer> premiereLigne = new HashMap<>();
        for (int i = 0; i < form.lignes().size(); i++) {
            LigneLivraisonDTO ligne = form.lignes().get(i);
            if (ligne == null) {
                result.reject("ligne.invalide", "Une ligne de la livraison est invalide.");
                continue;
            }
            if (ligne.produitId() == null) {
                continue; // déjà signalé par @NotNull
            }
            Integer dejaVu = premiereLigne.putIfAbsent(ligne.produitId(), i);
            if (dejaVu != null) {
                result.rejectValue("lignes[" + i + "].produitId", "doublon",
                        "Ce produit figure déjà à la ligne " + (dejaVu + 1)
                                + " : regroupez les quantités sur une seule ligne.");
            }
        }
    }
}